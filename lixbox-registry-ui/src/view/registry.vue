<template>
  <div>
    <confirm ref="confirm"></confirm>
    <v-row class="margin-10">
      <v-col md="3">
          <registryTree
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
              <h3 style="text-align:center">
                <v-text-field
                  :label="$t('registry.service.entry.name')"
                  class="headline"
                  style="max-width:30%"
                  placeholder="global-service-api-cache"
                  hide-details="auto"
                  v-model="selected.name"
                ></v-text-field>
              </h3>
              <div class="blue--text mb-2"></div>
              <div class="blue--text subheading font-weight-bold"></div>
            </v-card-text>
            <v-divider></v-divider>
            <v-card-text>
              <v-row class="text-align:center" tag="v-card-text">
                <v-text-field
                  :label="$t('registry.service.entry.version')"
                  style="max-width:max-content"
                  placeholder="1.0"
                  hide-details="auto"
                  v-model="selected.version"
                ></v-text-field>
              </v-row>
            </v-card-text>
            <v-row class="text-left" tag="v-card-text">
              <v-col class="mr-4" tag="strong" cols="12">
                {{$t('registry.service.entry.uris')}}
                <div
                  class="col-md-12"
                  v-for="(item, index) in this.selected.uris"
                  v-bind:key="item"
                >
                  <v-text-field
                    style="max-width: 60%;"
                    placeholder="tcp://localhost:6379"
                    hide-details="auto"
                    append-icon="mdi-delete"
                    :value="item"
                    @change="updateEntry(index, item, $event)"
                    @click:append="deleteEntry(index)"
                  ></v-text-field>
                </div>
              </v-col>
            </v-row>
            <v-card-actions>
              <v-btn color="warning" rounded outlined @click="addServiceUri">{{$t("registry.service.ui.button.entry.uri.add")}}</v-btn>
              <v-btn color="primary" rounded outlined @click="save">{{$t("registry.service.ui.button.entry.save")}}</v-btn>
              <v-btn color="error" rounded outlined @click="deleteServiceEntry">{{$t("registry.service.ui.button.entry.del")}}</v-btn>
            </v-card-actions>
          </v-card>
        </v-scroll-y-transition>
      </v-col>
    </v-row>
  </div>
</template>

<script>
/* eslint-disable */
import axios from 'axios';
import confirm from "@/components/ui/confirmDialog.vue";
import registryTree from "@/components/ui/registryTree.vue";
import { RegistryService } from "@/api/RegistryService.js";



export default {
  components: {
    registryTree,
    confirm
  },
  name: "RegistryView",
  data: () => ({
    registryUrl: 'http://localhost:18100/registry/api/1.0',
    entryKeys: [],
    selected: null,
    serviceEntryHeaders: [{ text: "Adresse", value: "uri" }],
    dialog: false,
    new: false,
    loading:true
  }),
  created() {
    this.initialize();
  },
  methods: {    
    getRegistryService(){
      return new RegistryService(this.registryUrl);
    },
    async getConfiguration(){      
      axios
        .get("/configuration")
        .then(response => response.data)
        .then(data => {
          this.registryUrl=data.registry;
        })
        .finally(function() {
          this.loading=false;
        });
    },
    initialize() {
      this.getConfiguration();
      this.getEntryKeys();
    },
    addServiceUri(){
      this.selected.uris.push('');
    },
    getEntryKeys() {
      this.getRegistryService().getEntryKeys().then(data => {
        this.entryKeys = data;
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
          this.selected = this.getRegistryService().getEntry(entry[0]).then(data => {
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
        await this.$refs.confirm.open(this.$t("registry.service.dialog.entry.uri.delete.title",[this.selected.name]), this.$t("registry.service.dialog.entry.uri.delete.text"), {
          color: this.$vuetify.theme.themes.light.error
        })
      ) 
      {
        this.selected.uris.splice(index, 1);
      }
    },
    save() {
      this.getRegistryService().synchronizeEntry(this.selected).then(data => {
        this.selected = data;
        if (this.new)
        {
          this.entryKeys.push(this.selected.name+":"+this.selected.version);
        }
      });
    },
    async deleteServiceEntry() {
      if (
        await this.$refs.confirm.open(this.$t("registry.service.dialog.entry.delete.title",[this.selected.name]), this.$t("registry.service.dialog.entry.delete.text"), {
          color: this.$vuetify.theme.themes.light.error
        })
      ) 
      {        
        let entryKey = this.selected.name+":"+this.selected.version;
        this.getRegistryService().removeEntry(this.selected).then(data => {
          if (data){
            this.entryKeys.splice(this.entryKeys.indexOf(entryKey),1);
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
