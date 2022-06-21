<template>
  <div class="wrapper">
    <parallax class="page-header header-filter" :style="headerStyle">
      <div class="md-layout">
        <div class="md-layout-item">
          <div class="image-wrapper">
            <img :src="leaf4" alt="leaf4" class="leaf4" v-show="leafShow" />
            <img :src="leaf3" alt="leaf3" class="leaf3" v-show="leafShow" />
            <img :src="leaf2" alt="leaf2" class="leaf2" v-show="leafShow" />
            <img :src="leaf1" alt="leaf1" class="leaf1" v-show="leafShow" />
            <div class="brand">
              <h1>VoD System</h1>
              <h3>A Video on Demand (VoD) System based on MinIO.</h3>
            </div>
          </div>
        </div>
      </div>
    </parallax>
    <div class="main main-raised">
      <div class="section">
        <div class="container">
          <div class="md-layout">
            <div
              class="md-layout-item md-size-66 md-xsmall-size-100 mx-auto text-center"
            >
              <h2 class="title text-center">Video List</h2>
              <h5 class="description">
                The videos shown below are deployed on a distributed
                object-based file system using open-source MinIO. The Springboot
                framework is used to provide RESTful APIs, while the Vue.js
                framework is applied to display the interface.
              </h5>
            </div>
          </div>
          <div
            class="features text-center"
            v-for="objs in objects"
            :key="objs.index"
          >
            <div class="md-layout">
              <div
                class="md-layout-item md-medium-size-33 md-small-size-100"
                v-if="listShow"
                v-for="obj in objs"
                :key="obj.name"
              >
                <div class="info">
                  <h4 class="info-title">{{ obj.name }}</h4>
                  <img
                    v-if="listShow"
                    :src="`${provider}/getFrame/${obj.name}/${indexFrame}`"
                    :alt="`${obj.name}`"
                  />
                  <div class="md-layout md-horizontal">
                    <div class="md-layout-item">
                      <router-link
                        :to="{ name: 'stage', params: { obj: obj.name } }"
                      >
                        <md-button class="md-primary md-sm">
                          Watch Online
                          <md-tooltip md-direction="bottom"
                            >Link to the video.</md-tooltip
                          >
                        </md-button>
                      </router-link>
                    </div>
                    <div class="md-layout-item">
                      <md-button
                        class="md-rose md-sm"
                        :id="`${obj.name}`"
                        @click="deleteConfirm($event)"
                      >
                        Delete
                        <md-tooltip md-direction="bottom"
                          >Delete the video.</md-tooltip
                        >
                      </md-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <modal v-if="deleteConfirmModal" @close="deleteConfirmModalHide">
            <template slot="header">
              <h4 class="modal-title">Confirm Deletion</h4>
              <md-button
                class="md-simple md-just-icon md-round modal-default-button"
                @click="deleteConfirmModalHide"
              >
                <md-icon>clear</md-icon>
              </md-button>
            </template>

            <template slot="body">
              <div class="alert alert-warning">
                <div class="container">
                  <div class="alert-icon">
                    <md-icon>warning</md-icon>
                  </div>
                  <b>WARNING</b>: Are you sure you want to delete
                  "{{ videoToDelete }}"
                   ? The operation is <b>not</b> recoverable.
                </div>
              </div>
            </template>

            <template slot="footer">
              <md-button class="md-danger md-simple" @click="deleteVideo"
                >Delete</md-button
              >
              <md-button class="md-simple" @click="deleteConfirmModalHide"
                >Don't delete</md-button
              >
            </template>
          </modal>
          <modal v-if="deleteCompleteModal" @close="deleteCompleteModalHide">
            <template slot="header">
              <h4 class="modal-title">Deletion Completed</h4>
              <md-button
                class="md-simple md-just-icon md-round modal-default-button"
                @click="deleteCompleteModalHide"
              >
                <md-icon>clear</md-icon>
              </md-button>
            </template>
            <template slot="body">
              <p>{{ videoToDelete }} has been deleted successfully.</p>
            </template>
          </modal>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Modal } from "@/components";

export default {
  name: "home",
  bodyClass: "index-page",
  components: {
    Modal
  },
  props: {
    image: {
      type: String,
      default: require("@/assets/img/vue-mk-header.jpg")
    },
    leaf4: {
      type: String,
      default: require("@/assets/img/leaf4.png")
    },
    leaf3: {
      type: String,
      default: require("@/assets/img/leaf3.png")
    },
    leaf2: {
      type: String,
      default: require("@/assets/img/leaf2.png")
    },
    leaf1: {
      type: String,
      default: require("@/assets/img/leaf1.png")
    },
    header: {
      type: String,
      default: require("@/assets/img/bg7.jpg")
    }
  },
  data() {
    return {
      leafShow: false,
      deleteConfirmModal: false,
      deleteCompleteModal: false,
      videoToDelete: null,
      provider: "http://localhost:8079",
      listShow: false,
      objects: null,
      indexFrame: 640
    };
  },
  methods: {
    leafActive() {
      if (window.innerWidth < 768) {
        this.leafShow = false;
      } else {
        this.leafShow = true;
      }
    },
    listVideos() {
      this.$http.get(this.provider + "/videos").then(
        response => {
          this.objects = response.data;
          this.listShow = true;
        },
        response => {
          console.log("error");
        }
      );
    },
    deleteConfirm(e) {
      this.videoToDelete = e.currentTarget.id;
      this.deleteConfirmModal = true;
    },
    deleteVideo() {
      this.deleteConfirmModal = false;
      this.$http
        .post(
          this.provider + "/removeObject",
          { object: this.videoToDelete },
          { emulateJSON: true }
        )
        .then(
          () => {},
          response => {
            console.log(response.data);
          }
        );
      this.deleteCompleteModal = true;
      this.listVideos();
    },
    deleteConfirmModalHide() {
      this.deleteConfirmModal = false;
      this.videoToDelete = null;
    },
    deleteCompleteModalHide() {
      this.deleteCompleteModal = false;
      this.videoToDelete = null;
    }
  },
  computed: {
    headerStyle() {
      return {
        backgroundImage: `url(${this.image})`
      };
    },
  },
  mounted() {
    this.leafActive();
    this.listVideos();
    window.addEventListener("resize", this.leafActive);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.leafActive);
  }
};
</script>
<style lang="scss">
.section-download {
  .md-button + .md-button {
    margin-left: 5px;
  }
}

@media all and (min-width: 991px) {
  .btn-container {
    display: flex;
  }
}
</style>
