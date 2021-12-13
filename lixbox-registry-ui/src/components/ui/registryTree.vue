<template>
  <v-card rounded style="max-width:auto; margin-right:1rem">
    <v-sheet class="pa-4 primary lighten-2">
      <v-text-field
        v-model="search"
        :label="$t('registry.service.action.search.placeholder')"
        dark
        flat
        solo-inverted
        hide-details
        clearable
        clear-icon="mdi-close-circle-outline"
      ></v-text-field>
    </v-sheet>
    <v-card-text :class="this.cssClass">
      <v-treeview
        :items="parsedItems"
        :return-object="returnObject"
        :search="search"
        activatable
        open-on-click
        open-all
        transition
        @update:active="pushActiveEvent"
      >
        <template v-slot:prepend="{ item, open }">
          <v-icon v-if="!item.file">{{ open ? 'mdi-folder-open' : 'mdi-folder' }}</v-icon>
          <v-icon v-else>{{ "mdi-nodejs" }}</v-icon>
        </template>
      </v-treeview>
    </v-card-text>
  </v-card>
</template>

<script>
export default {
  name: "registryTree",
  props: ["cssClass", "opens", "items"],
  data: () => ({
    active: [],
    cParsedItems: [],
    selected: [],
    search: null,
    computedOpens: [],
    returnObject: true,
    actionsAdded: false
  }),
  computed: {
    filter(item, search) {
      alert(item + " " + search);
      return 1;
    },
    parsedItems() {
      this.clearParsedItems();
      this.items.forEach(element => {
        this.parseItem(element);
      });
      this.addActions();
      return this.cParsedItems;
    }
  },
  methods: {
    clearParsedItems() {
      this.cParsedItems = [];
    },
    addActions() {
      this.cParsedItems.push({
        name: this.$t('registry.service.action.add.text'),
        file: " ",
        action: "addEntry"
      });
    },
    pushActiveEvent: function(event) {
      if (event.length > 0) {
        if (event[0].action == "addEntry") {
          this.$emit("update:active", [{id:"", name: "", version: "", key: "new" }]);
        } else {
          this.$emit("update:active", event);
        }
      }
    },
    parseItem(item) {
      var global = item.split(":"),
        split = global[0].split("-"),
        porteeKey = split[0],
        modeKey = split[1],
        typeKey = split[2],
        nameKey = split[3],
        versionKey = global[1];

      //traitement de la portee
      var found = false;
      var portee;
      this.cParsedItems.forEach(porteeTmp => {
        if (porteeTmp.name == porteeKey) {
          found = true;
          portee = porteeTmp;
        }
      });
      if (!found) {
        let init = {id:"", name: porteeKey, children: [] };
        let pos = this.cParsedItems.push(init);
        portee = this.cParsedItems[pos - 1];
        portee.id = pos;
      }

      //traitement du mode
      found = false;
      var mode;
      portee.children.forEach(modeTmp => {
        if (modeTmp.name == modeKey) {
          found = true;
          mode = modeTmp;
        }
      });
      if (!found) {
        let init = {id:"", name: modeKey, children: [] };
        let pos = portee.children.push(init);
        mode = portee.children[pos - 1];
        mode.id=portee.id+"-"+pos;
      }

      //traitement du type
      found = false;
      var type;
      mode.children.forEach(typeTmp => {
        if (typeTmp.name == typeKey) {
          found = true;
          type = typeTmp;
        }
      });
      if (!found) {
        let init = {id:"", name: typeKey, children: [] };
        let pos = mode.children.push(init);
        type = mode.children[pos - 1];
        type.id=mode.id+"-"+pos;
      }

      //traitement du name
      found = false;
      var name;
      type.children.forEach(nameTmp => {
        if (nameTmp.name == nameKey + "-" + versionKey) {
          found = true;
        }
      });
      if (!found) {
        let init = {id:"", name: nameKey + "-" + versionKey, file: " ", key: item };
        let pos = type.children.push(init);
        name = type.children[pos - 1];
        name.id=type.id+"-"+pos;
      }

      //Traitement des opens
      this.computedOpens = [];
      this.opens.forEach(element => {
        if (element == porteeKey) {
          this.computedOpens.push(portee);
        }
        if (element == modeKey) {
          this.computedOpens.push(mode);
        }
        if (element == typeKey) {
          this.computedOpens.push(type);
        }
      });
    }
  }
};
</script>