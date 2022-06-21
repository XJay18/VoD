package com.xjay.vod.controller;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@RestController
@CrossOrigin(origins = {"http://localhost:8080"})
public class MinioController {
    @Autowired
    private MinioClient minioClient;
    private final Map<String, String> etag2url = new HashMap<>();
    private final Logger logger = Logger.getLogger("MinioController");
    private static final String BUCKET = "test";
    private static final int NUM_VIDEOS_PROW = 3;


    @GetMapping("/list")
    public List<Object> list() throws Exception {
        Iterable<Result<Item>> myObjects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(BUCKET)
                        .recursive(true)
                        .build()
        );
        Iterator<Result<Item>> iterator = myObjects.iterator();
        List<Object> items = new ArrayList<>();
        String format = "{'name':'%s','size':'%s'}";
        while (iterator.hasNext()) {
            Item item = iterator.next().get();
            items.add(JSON.parse(String.format(format, item.objectName(), formatFileSize(item.size()))));
        }
        return items;
    }

    @GetMapping("/videos")
    public List<List<Object>> videos() throws Exception {
        Iterable<Result<Item>> myObjects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(BUCKET)
                        .recursive(true)
                        .build()
        );
        Iterator<Result<Item>> iterator = myObjects.iterator();
        List<List<Object>> items = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        String format = "{'name':'%s','size':'%s'}";
        while (iterator.hasNext()) {
            if (row.size() == NUM_VIDEOS_PROW) {
                items.add(row);
                row = new ArrayList<>();
            }
            Item item = iterator.next().get();
            if (isVideo(item.objectName()))
                row.add(JSON.parse(String.format(format, item.objectName(), formatFileSize(item.size()))));
        }
        if (row.size() > 0)
            items.add(row);
        return items;
    }

    @PostMapping(value = "/info")
    public Map<String, Object> objectInfo(HttpServletResponse response,
                                          @RequestParam("object") String object,
                                          @RequestParam(value = "url", required = false, defaultValue = "false")
                                          String requireURL) {
        Map<String, Object> map = new HashMap<>();
        try {
            map = getObjectInfo(object);
            if (Boolean.parseBoolean(requireURL)) {
                String etag = map.get("etag").toString();
                String url = etag2url.getOrDefault(etag, null);
                if (url == null) {
                    Map<String, String> extraParams = new HashMap<>();
                    extraParams.put("response-content-type", map.get("content-type").toString());
                    url = minioClient.getPresignedObjectUrl(
                            GetPresignedObjectUrlArgs.builder()
                                    .method(Method.GET)
                                    .bucket(BUCKET)
                                    .object(object)
                                    .expiry(1, TimeUnit.DAYS)
                                    .extraQueryParams(extraParams)
                                    .build()
                    );
                    etag2url.put(etag, url);
                }
                map.put("url", url);
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
            response.setStatus(500);
            map.put("error", e.getMessage());
        }
        return map;
    }


    @RequestMapping(value = {"/getFrame/{obj}/{frameIndex}", "/getFrame/{obj}"})
    public void getVideoFrame(HttpServletResponse response,
                              @PathVariable("obj") String object,
                              @PathVariable(value = "frameIndex", required = false)
                              Integer frameIndex
    ) {
        InputStream frameStream = null;
        if (frameIndex == null)
            frameIndex = 1;
        try {
            Map<String, Object> map = getObjectInfo(getImageName(object));
            map.put("content-type", "image/jpeg");
            frameStream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(BUCKET).object(getImageName(object)).build()
            );
            IOUtils.copy(frameStream, response.getOutputStream());
        } catch (ErrorResponseException ere) {
            if (ere.errorResponse().code().equals("NoSuchKey")) {
                uploadVideoFrame(response, object, frameIndex);
            } else {
                logger.warning(ere.getMessage());
                response.setStatus(500);
                response.addHeader("error", ere.getMessage());
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
            response.setStatus(500);
            response.addHeader("error", e.getMessage());
        } finally {
            if (frameStream != null) {
                try {
                    frameStream.close();
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    @RequestMapping("/getObject/{obj}")
    public void getObject(HttpServletResponse response,
                          @PathVariable("obj") String object) {
        InputStream in = null;
        try {
            Map<String, Object> map = getObjectInfo(object);
            for (String key : map.keySet()) {
                response.addHeader(key, map.get(key).toString());
            }
            in = minioClient.getObject(
                    GetObjectArgs.builder().bucket(BUCKET).object(object).build()
            );
            IOUtils.copy(in, response.getOutputStream());
        } catch (Exception e) {
            logger.warning(e.getMessage());
            response.setStatus(500);
            response.addHeader("error", e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    @PostMapping("/removeObject")
    public Map<String, Object> removeObject(HttpServletResponse response,
                                            @RequestParam("object") String object) {
        Map<String, Object> map = new HashMap<>();
        try {
            map = getObjectInfo(object);
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(BUCKET).object(object).build()
            );
            map.put("removed", true);
            map.put("size", map.get("size"));
            map.put("etag", map.get("etag"));
        } catch (Exception e) {
            logger.warning(e.getMessage());
            response.setStatus(404);
            map.put("removed", false);
            map.put("error", e.getMessage());
        }
        return map;
    }

    private boolean isVideo(String object) {
        boolean value = false;
        try {
            String contentType = getObjectInfo(object).get("content-type").toString();
            return contentType.startsWith("video/");
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }
        return value;
    }

    private void uploadVideoFrame(HttpServletResponse response,
                                  String object,
                                  Integer frameIndex) {
        InputStream objStream = null;
        InputStream frameStream = null;
        FFmpegLogCallback.setLevel(avutil.AV_LOG_QUIET);
        try {
            Map<String, Object> map = getObjectInfo(object);
            map.put("content-type", "image/jpeg");
            for (String key : map.keySet()) {
                response.addHeader(key, map.get(key).toString());
            }
            objStream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(BUCKET).object(object).build()
            );
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(objStream, 0);
                 Java2DFrameConverter converter = new Java2DFrameConverter()
            ) {
                grabber.start();
                int i = 0;
                Frame frame = null;
                if (frameIndex > grabber.getLengthInFrames() || frameIndex <= 0)
                    frame = grabber.grabFrame();
                else {
                    while (i < grabber.getLengthInFrames()) {
                        frame = grabber.grabFrame();
                        i += 1;
                        if (i >= frameIndex && frame.image != null)
                            break;
                    }
                }
                // fallback strategy
                if (frame == null || frame.image == null) {
                    frameStream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(BUCKET)
                                    .object("frames/default_frame.jpg")
                                    .build()
                    );
                    IOUtils.copy(frameStream, response.getOutputStream());
                }
                BufferedImage srcImg = converter.getBufferedImage(frame);
                int width = 800;
                int height = (int) (((double) width / srcImg.getWidth()) * srcImg.getHeight());
                BufferedImage tgtImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                tgtImg.getGraphics().drawImage(srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
                ImageIO.write(tgtImg, "jpg", response.getOutputStream());
                // Optionally upload the captured frame to MinIo
                Map<String, String> userMetaData = new HashMap<>();
                userMetaData.put("frame-index", frameIndex.toString());
                userMetaData.put("created-time", (new Date()).toString());
                ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
                ImageIO.write(tgtImg, "jpg", tmpStream);
                frameStream = new ByteArrayInputStream(tmpStream.toByteArray());
                putObject(frameStream, getImageName(object), userMetaData);
                grabber.stop();
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
            response.setStatus(500);
            response.addHeader("error", e.getMessage());
        } finally {
            if (objStream != null) {
                try {
                    objStream.close();
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
            if (frameStream != null) {
                try {
                    frameStream.close();
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
    }

    private Map<String, Object> getObjectInfo(String object)
            throws
            ErrorResponseException,
            InsufficientDataException,
            InternalException,
            InvalidKeyException,
            InvalidResponseException,
            IOException,
            NoSuchAlgorithmException,
            ServerException,
            XmlParserException {
        Map<String, Object> map = new HashMap<>();
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder().bucket(BUCKET).object(object).build()
        );
        map.put("content-disposition", "attachment;filename=" + URLEncoder.encode(object, "UTF-8"));
        map.put("etag", stat.etag());
        map.put("size", formatFileSize(stat.size()));
        map.put("user-meta-data", stat.userMetadata());
        map.put("content-type", stat.contentType());
        return map;
    }

    private void putObject(InputStream in, String object)
            throws
            ErrorResponseException,
            InsufficientDataException,
            InternalException,
            InvalidKeyException,
            InvalidResponseException,
            IOException,
            NoSuchAlgorithmException,
            ServerException,
            XmlParserException {
        putObject(in, object, new HashMap<>());
    }

    private void putObject(InputStream in, String object, Map<String, String> metaData)
            throws
            ErrorResponseException,
            InsufficientDataException,
            InternalException,
            InvalidKeyException,
            InvalidResponseException,
            IOException,
            NoSuchAlgorithmException,
            ServerException,
            XmlParserException {
        minioClient.putObject(
                PutObjectArgs.builder().bucket(BUCKET)
                        .object(object)
                        .stream(in, -1, 10485760)
                        .userMetadata(metaData)
                        .build()
        );
    }

    private static String getImageName(String object) {
        String[] strs = object.split("\\.");
        strs[strs.length - 1] = "jpg";
        return "frames/" + String.join(".", strs);
    }

    private static String formatFileSize(long fileS) {
        final long gap = 1024;
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString;
        String zeroSize = "0B";
        if (fileS == 0) {
            return zeroSize;
        }
        if (fileS < gap) {
            fileSizeString = df.format((double) fileS) + " B";
        } else if (fileS < gap * gap) {
            fileSizeString = df.format((double) fileS / gap) + " KB";
        } else if (fileS < gap * gap * gap) {
            fileSizeString = df.format((double) fileS / gap / gap) + " MB";
        } else {
            fileSizeString = df.format((double) fileS / gap / gap / gap) + " GB";
        }
        return fileSizeString;
    }
}
