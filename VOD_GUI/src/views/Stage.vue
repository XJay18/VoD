<template>
  <div class="wrapper">
    <parallax class="section page-header header-filter" :style="headerStyle">
      <div class="container">
        <div class="md-layout">
          <div
            class="md-layout-item md-size-66 md-xsmall-size-100 mx-auto text-center"
          >
            <h1 class="title text-center">{{ $route.params.obj }}</h1>
          </div>
          <div
            class="md-layout-item md-size-50 md-small-size-70 md-xsmall-size-100"
          >
            <h4 v-if="showInfo">
              <b>File information:</b>
            </h4>
            <h4 v-if="showInfo">ETAG: {{ this.etag }}</h4>
            <h4 v-if="showInfo">Size: {{ this.size }}</h4>
            <h4 v-if="showInfo">
              Access URL:
              <p>{{ this.url }}</p>
            </h4>
          </div>
          <div
            class="md-layout-item md-size-50 md-small-size-70 md-xsmall-size-100"
          >
            <video
              :src="`${provider}/getObject/${this.$route.params.obj}`"
              controls="controls"
              controlsList="nodownload"
              preload="auto"
            />
          </div>
          <div
            v-if="showInfo"
            class="md-layout-item md-layout md-gutter md-alignment-center-center md-size-50 md-small-size-70 md-xsmall-size-100"
          >
            <div
              v-if="showInfo"
              class="md-layout-item md-layout md-alignment-center-center"
            >
              <md-button class="md-success md-sm " @click="showQRCode()">
                <md-icon>qr_code</md-icon> Display QR Code
              </md-button>
            </div>
            <div
              v-if="showInfo"
              class="md-layout-item md-layout md-alignment-center-center"
            >
              <router-link :to="{ name: 'home' }">
                <md-button class="md-default md-sm" @click="showQRCode()">
                  <md-icon>home</md-icon> Return HOME
                </md-button>
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </parallax>
    <modal v-if="qrModal" @close="qrModalHide">
      <template slot="header">
        <h4 class="modal-title">Scan the QR Code below</h4>
        <md-button
          class="md-simple md-just-icon md-round modal-default-button"
          @click="qrModalHide"
        >
          <md-icon>clear</md-icon>
        </md-button>
      </template>
      <template slot="body">
        <vue-qr
          :text="`${url}`"
          :logoSrc="`${provider}/getObject/xiaohei.jpg`"
        ></vue-qr>
      </template>
    </modal>
  </div>
</template>

<script>
import VueQr from "vue-qr";
import { Modal } from "@/components";

export default {
  name: "Stage",
  bodyClass: "landing-page",
  components: {
    VueQr,
    Modal
  },
  props: {
    header: {
      type: String,
      default: require("@/assets/img/bg7.jpg")
    }
  },
  data() {
    return {
      provider: "http://localhost:8079",
      etag: null,
      size: null,
      url: null,
      qrModal: false,
      showInfo: false
    };
  },
  computed: {
    headerStyle() {
      return {
        backgroundImage: `url(${this.header})`
      };
    }
  },
  methods: {
    getObjectInfo(obj, requireUrl) {
      this.$http
        .post(
          this.provider + "/info",
          { object: obj, url: requireUrl },
          { emulateJSON: true }
        )
        .then(
          response => {
            this.etag = response.data.etag;
            this.size = response.data.size;
            this.url = response.data.url;
          },
          response => {
            console.log("error");
          }
        );
      this.showInfo = true;
    },
    showQRCode() {
      this.qrModal = true;
    },
    qrModalHide() {
      this.qrModal = false;
    }
  },
  mounted() {
    this.getObjectInfo(this.$route.params.obj, true);
  }
};
</script>

<style lang="scss" scoped>
.md-card-actions.text-center {
  display: flex;
  justify-content: center !important;
}
.contact-form {
  margin-top: 30px;
}
.md-has-textarea + .md-layout {
  margin-top: 15px;
}
</style>
