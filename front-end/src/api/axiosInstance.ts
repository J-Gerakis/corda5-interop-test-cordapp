import axios, { AxiosRequestConfig } from "axios";

axios.interceptors.request.use(async (config: AxiosRequestConfig) => {
  const app = sessionStorage.getItem("app");

  let APIURL = "";

  switch (app) {
    case "alice-fruit":
      APIURL = "alicefruit/api/v1/";
      break;
    case "bob-fruit":
      APIURL = "bobfruit/api/v1/";
      break;
    case "alice-token":
      APIURL = "alicetoken/api/v1/";
      break;
    case "bob-token":
      APIURL = "bobtoken/api/v1/";
      break;
    default:
      console.error("No App Selected");
  }

  config.baseURL = APIURL;

  config.headers = {
    accept: "application/json",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,PUT,POST,DELETE,PATCH,OPTIONS",
  };

  config.auth = {
    username: "user1",
    password: "test",
  };

  return config;
});

export default axios;
