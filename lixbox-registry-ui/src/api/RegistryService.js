import axios from "axios";

export class RegistryService {
  API_URL = "";

  constructor(url) {
    this.API_URL = url;
  }

  getEntryKeys() {
    const url = `${this.API_URL}/entries/keys/`;
    return axios.get(url).then((response) => response.data);
  }

  getEntry(item) {
    const url = this.API_URL + "/entries/" + item.key;
    return axios.get(url).then((response) => response.data);
  }

  synchronizeEntry(entry) {
    const url = this.API_URL + "/entry/sync";
    return axios.post(url, entry).then((response) => response.data);
  }

  removeEntry(entry) {
    const url = this.API_URL + "/entry/" + entry.name + "/" + entry.version;
    return axios.delete(url).then((response) => response.data);
  }
}
