<template>
  <div>
    <div class="text-center">
      <v-dialog v-model="dialog" max-width="500">
        <v-card>
          <v-card-title class="headline">{{$t('registry.service.instance.bilan')}}</v-card-title>
          <v-card-text>
            <json-viewer :value="healthState" :expand-depth=5></json-viewer>
          </v-card-text>
        </v-card>
      </v-dialog>
    </div>
    <confirm ref="confirm"></confirm>
    <v-row class="margin-10">
      <v-col md="3">
        <registryTree
          cssClass="tree"
          :opens="['global','service', 'api']"
          :items="entryKeys"
          @update:active="select"
        />
      </v-col>
      <v-col class="col-md-9 text-center">
        <v-scroll-y-transition mode="out-in">
          <div
            v-if="!selected"
            class="title grey--text text--lighten-1 font-weight-light"
            style="align-self: center;"
          >{{$t('registry.service.select')}}</div>
          <v-card v-else :key="selected.id" class="pt-6 mx-auto" flat>
            <v-card-text>
              <h3 style="text-align:center; display:flex; flex-direction:row">
                <v-text-field
                  :label="$t('registry.service.entry.name')"
                  class="headline"
                  style="max-width:30%; margin-right:30px"
                  placeholder="global-service-api-cache"
                  hide-details="auto"
                  v-model="selected.name"
                ></v-text-field>
                <span v-if="selected.status=='UP'" class="v-input headline v-input--hide-details v-input--is-label-active v-input--is-dirty theme--light v-text-field v-text-field--is-booted v-text-field--placeholder" style="flex-direction:row;display:flex;color:green;">UP</span>
                <span v-else class="v-input headline v-input--hide-details v-input--is-label-active v-input--is-dirty theme--light v-text-field v-text-field--is-booted v-text-field--placeholder" style="flex-direction:row;display:flex;color:red;">DOWN</span>
              </h3>
              <div class="blue--text mb-2"></div>
              <div class="blue--text subheading font-weight-bold"></div>
            </v-card-text>
            <v-divider></v-divider>
            <v-card-text>
              <v-row class="text-align:center" tag="v-card-text">
                <v-col class="d-flex" cols="6" sm="6">
                  <v-text-field
                    :label="$t('registry.service.entry.version')"
                    style="max-width:max-content"
                    placeholder="1.0"
                    hide-details="auto"
                    v-model="selected.version"
                  ></v-text-field>
                </v-col>
                <v-col class="d-flex" cols="6" sm="6">
                  <v-select
                    :items="serviceTypes"
                    :label="$t('registry.service.entry.type')"
                    style="max-width:max-content"
                    placeholder="TCP"
                    hide-details="auto"
                    v-model="selected.type"
                  ></v-select>
                </v-col>
              </v-row>

              <v-row class="text-align:center" tag="v-card-text">
                <v-col class="d-flex" cols="12" sm="12">
                  <v-text-field
                    :label="$t('registry.service.entry.endpoint')"
                    placeholder="http://www.google.fr:443"
                    hide-details="auto"
                    v-model="selected.endpointUri"
                  ></v-text-field>
                </v-col>
              </v-row>
            </v-card-text>

            <v-row class="text-left" tag="v-card-text">
              <v-col class="d-flex" cols="12" sm="12" md="12">
                <v-simple-table style="width:100%">
                  <template v-slot:default>
                    <thead>
                      <tr>
                        <th class="text-left">URI</th>
                        <th class="text-left">Live</th>
                        <th class="text-left">Ready</th>
                        <th class="text-left">Live State</th>
                        <th class="text-left">Ready State</th>
                        <th class="text-left">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(item, index) in selected.instances" :key="item.live">
                        <td>
                          <v-text-field
                            :label="$t('registry.service.instance.uri')"
                            placeholder="http://www.google.fr:443"
                            hide-details="auto"
                            v-model="item.uri"
                          ></v-text-field>
                        </td>
                        <td>
                          <span v-if="item.live==true" style="color:green;">UP</span>
                          <span v-else style="color:red;">DOWN</span>
                        </td>
                        <td>
                          <span v-if="item.ready==true" style="color:green;">UP</span>
                          <span v-else style="color:red;">DOWN</span>
                        </td>
                        <td> 
                          <v-btn class="ma-2" outlined fab color="info" @click.stop="viewHealthState(item.liveState)"><v-icon>mdi-format-list-bulleted-square</v-icon></v-btn>
                        </td>
                        <td> 
                          <v-btn class="ma-2" outlined fab color="info" @click.stop="viewHealthState(item.readyState)"><v-icon>mdi-format-list-bulleted-square</v-icon></v-btn>
                        </td>
                        <td>
                          <v-btn class="ma-2" outlined fab color="error" @click.stop="deleteEntry(index)"><v-icon>mdi-delete</v-icon></v-btn>
                        </td>
                      </tr>
                    </tbody>
                  </template>
                </v-simple-table>
              </v-col>
            </v-row>
            <v-card-actions>
              <v-btn
                color="warning"
                rounded
                outlined
                @click="addServiceInstance"
              >{{$t("registry.service.ui.button.entry.uri.add")}}</v-btn>
              <v-btn
                color="primary"
                rounded
                outlined
                @click="save"
              >{{$t("registry.service.ui.button.entry.save")}}</v-btn>
              <v-btn
                color="error"
                rounded
                outlined
                @click="deleteServiceEntry"
              >{{$t("registry.service.ui.button.entry.del")}}</v-btn>
            </v-card-actions>
          </v-card>
        </v-scroll-y-transition>
      </v-col>
    </v-row>
  </div>
</template>

<script>
/* eslint-disable */
import axios from "axios";
import JsonViewer from 'vue-json-viewer'
import confirm from "@/components/ui/confirmDialog.vue";
import registryTree from "@/components/ui/registryTree.vue";
import { RegistryService } from "@/api/RegistryService.js";

export default {
  components: {
    registryTree,
    confirm,
    JsonViewer
  },
  name: "RegistryView",
  data: () => ({
    registryUrl: process.env.VUE_APP_REGISTRY_URI,
    entryKeys: [],
    selected: null,
    serviceEntryHeaders: [{ text: "Adresse", value: "uri" }],
    serviceTypes: ["MANUAL","TCP", "HTTP", "MICRO_PROFILE"],
    dialog: false,
    new: false,
    loading: true,
    healthState:""
  }),
  created() {
    this.initialize();
  },
  methods: {
    viewHealthState(state){
      this.healthState = state;
      this.dialog=true;
    },
    getRegistryService() {
      return new RegistryService(this.registryUrl);
    },
    getConfiguration() {
      axios
        .get(process.env.VUE_APP_CONFIGURATION_URI)
        .then(reponse => reponse.data)
        .then(data => {
          this.registryUrl = data.registry.api.uri;
          this.getEntryKeys();
        })
        .catch(error =>{
          this.getEntryKeys();
        });
    },
    initialize() {
      this.getConfiguration();
    },

    addServiceInstance() {
      this.selected.instances.push({"uri":""});
    },
    getEntryKeys() {
      this.getRegistryService()
        .getEntryKeys()
        .then(data => {
          this.entryKeys = data;
        })
        .catch(error=>{
          alert(JSON.stringify(error));
        });
    },
    select(entry) {
      if (entry.length == 0) {
        this.selected = null;
      } else {
        if (entry[0].key == "new") {
          this.selected = { name: "", version: "", uris: [] };
          this.new = true;
        } else {
          this.selected = this.getRegistryService()
            .getEntry(entry[0])
            .then(data => {
              this.selected = data[0];
            });
        }
      }
    },
    async updateEntry(index, oldValue, newValue) {
      this.selected.uris[index] = newValue;
    },
    async deleteEntry(index) {
      if (
        await this.$refs.confirm.open(
          this.$t("registry.service.dialog.entry.uri.delete.title", [
            this.selected.name
          ]),
          this.$t("registry.service.dialog.entry.uri.delete.text"),
          {
            color: this.$vuetify.theme.themes.light.error
          }
        )
      ) {
        this.selected.instances.splice(index, 1);
      }
    },
    save() {
      this.getRegistryService()
        .synchronizeEntry(this.selected)
        .then(data => {
          this.selected = data;
          if (this.new) {
            this.entryKeys.push(
              this.selected.name + ":" + this.selected.version
            );
          }
        });
    },
    async deleteServiceEntry() {
      if (
        await this.$refs.confirm.open(
          this.$t("registry.service.dialog.entry.delete.title", [
            this.selected.name
          ]),
          this.$t("registry.service.dialog.entry.delete.text"),
          {
            color: this.$vuetify.theme.themes.light.error
          }
        )
      ) {
        let entryKey = this.selected.name + ":" + this.selected.version;
        this.getRegistryService()
          .removeEntry(this.selected)
          .then(data => {
            if (data) {
              this.entryKeys.splice(this.entryKeys.indexOf(entryKey), 1);
            }
          });
      }
    }
  }
};
</script>

<style scoped>
.margin-5 {
  margin: 0.5rem 0.5rem 0 0.5rem;
}
.margin-10 {
  margin: 1rem 1rem 0 1rem;
}
</style>
<style>
.tree {
    height: 80vh;
    position: absolute;
    overflow-y: auto;
}
</style>
