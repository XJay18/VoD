<template>
  <div class="wrapper">
    <parallax class="section page-header header-filter" :style="headerStyle">
      <div class="container">
        <div class="md-layout">
          <div
            class="md-layout-item md-size-66 md-xsmall-size-100 mx-auto text-center"
          >
            <h1 class="title text-center">Documentation</h1>
          </div>
        </div>
      </div>
    </parallax>
    <div class="main main-raised">
      <div class="section profile-content">
        <div class="container">
          <div class="md-layout">
            <div
              class="description md-layout-item md-size-80 md-small-size-90 md-xsmall-size-100"
            >
              <h4 class="description">
                <b>Overall Information:</b>
                This web site provides <b>Video on Demand (VoD)</b> Service with
                <a href="https://min.io/" target="_blank">MinIO</a> as the object-based file
                storage system. The backend is implemented using
                <a href="https://spring.io/projects/spring-boot" target="_blank">SpringBoot</a>
                framework while the front end is built up with
                <a href="https://vuejs.org/" target="_blank">Vue.js</a> and
                <a href="https://github.com/vuematerial/vue-material" target="_blank"
                  >Vue Material</a
                >
                .
              </h4>
            </div>
            <div
              class="description md-layout-item md-size-80 md-small-size-90 md-xsmall-size-100"
            >
              <h4 class="description">
                <b>Home Page:</b>
                You can browse the available videos at
                <a href="#/home">home</a> page. I only display one of the frames
                from the video at this page like the screenshot shown below. You
                can see that, for each video in the list, you may click
                <b class="text-primary">watch online</b> button to watch the
                video on the platform. Or, you may click
                <b class="text-rose">delete</b> button to delete the video on
                the list. Note that, once you delete the video, you cannot
                retrieve it.
              </h4>
            </div>
            <div
              class="md-layout-item md-layout md-alignment-center md-size-80 md-small-size-90 md-xsmall-size-100 mx-auto"
            >
              <img :src="exampleHome" class="rounded" />
            </div>
            <div
              class="description md-layout-item md-size-80 md-small-size-90 md-xsmall-size-100"
            >
              <h4 class="description">
                <b>Stage Page:</b>
                After clicking <b class="text-primary">watch online</b>, you
                will be redirected to a new page. You can find the information
                of the video and watch it online. An example is shown below.
              </h4>
            </div>
            <div
              class="md-layout-item md-layout md-alignment-center md-size-50 md-small-size-65 md-xsmall-size-80 mx-auto"
            >
              <img :src="exampleStage" class="rounded" />
            </div>
          </div>
          <div class="profile-tabs">
            <div class="md-layout">
              <div
                class="md-layout-item md-size-66 md-xsmall-size-100 mx-auto text-center"
              >
                <h3 class="title text-center">Contact Information</h3>
              </div>
            </div>
            <tabs
              :tab-name="['Wechat', 'Email']"
              :tab-icon="['wechat', 'email']"
              plain
              nav-pills-icons
              color-button="success"
            >
              <!-- here you can add your content for tab-content -->
              <template slot="tab-pane-1">
                <div class="md-layout">
                  <div
                    class="md-layout-item md-size-25 md-xsmall-size-33 mx-auto"
                  >
                    <img :src="wechat" class="rounded" />
                  </div>
                </div>
              </template>
              <template slot="tab-pane-2">
                <div class="md-layout">
                  <div class="text-center md-layout-item mx-auto">
                    <h4>
                      Institution: junyicao@sjtu.edu.cn
                    </h4>
                    <h4>
                      Private: xjay2018@gmail.com
                    </h4>
                  </div>
                </div>
              </template>
            </tabs>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Tabs } from "@/components";
export default {
  components: {
    Tabs
  },
  bodyClass: "profile-page",
  props: {
    header: {
      type: String,
      default: require("@/assets/img/city-profile.jpg")
    },
    wechat: {
      type: String,
      default: require("@/assets/img/wechat.jpg")
    },
    exampleHome: {
      type: String,
      default: require("@/assets/img/example_home.png")
    },
    exampleStage: {
      type: String,
      default: require("@/assets/img/example_stage.png")
    }
  },
  computed: {
    headerStyle() {
      return {
        backgroundImage: `url(${this.header})`
      };
    }
  }
};
</script>

<style lang="scss" scoped>
.section {
  padding: 0;
}
.description {
  text-align: justify;
}

.profile-tabs::v-deep {
  .md-card-tabs .md-list {
    justify-content: center;
  }

  [class*="tab-pane-"] {
    margin-top: 3.213rem;
    padding-bottom: 50px;

    img {
      margin-bottom: 2.142rem;
    }
  }
}
</style>
