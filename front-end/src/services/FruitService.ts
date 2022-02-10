import axios from "../api/axiosInstance";
import { Fruit } from "../models/models";

export const fetchAllFruits = async (identity: string): Promise<Fruit[]> => {
  if (identity === "alice") {
    sessionStorage.setItem("app", "alice-fruit");
  } else {
    sessionStorage.setItem("app", "bob-fruit");
  }

  const response = await axios.post("persistence/query", {
    request: {
      namedParameters: {},
      queryName: "FruitSchemaV1.PersistentFruit.ListAll",
    },
    context: {
      awaitForResultTimeout: "PT15M",
      currentPosition: -1,
      maxCount: 100,
    },
  });

  const positionedValues = response.data.positionedValues;

  let fruits: Fruit[] = positionedValues.map((item: any) => {
    const rawFruit = JSON.parse(item.value.json);
    const fruit: Fruit = {
      fruitType: rawFruit.fruitType,
      quantity: rawFruit.quantity,
      message: rawFruit.message,
      ownerName: rawFruit.ownerName,
      timestamp: rawFruit.timestamp,
      linearId: rawFruit.linearId,
      status: "available",
    };
    return fruit;
  });

  return fruits;
};

export const addFruit = async (
  type: string,
  quantity: number,
  message: string
) => {
  // only alice can issue new fruit
  sessionStorage.setItem("app", "alice-fruit");

  const clientId = `clientId-${Math.random()}`;

  const response = await axios.post(`flowstarter/startflow`, {
    rpcStartFlowRequest: {
      clientId: clientId,
      flowName: "net.corda.fruit.flows.IssueFruitFlow",
      parameters: {
        parametersInJson: `{"fruitType": "${type}", "quantity": "${quantity}", "message":"${message}"}`,
      },
    },
  });

  return response.data;
};

const FruitService = {
  fetchAllFruits,
  addFruit,
};

export default FruitService;
