import React, { createContext, useState } from "react";

import { Fruit } from "../models/models";
import FruitService from "../services/FruitService";

interface FruitContextInterface {
  fruitsAlice: Fruit[];
  fruitsBob: Fruit[];
  refreshFruitsInventory: (identity: string) => void;
  addFruit: (type: string, quantity: number, message: string) => void;
}

const contextDefaultValues: FruitContextInterface = {
  fruitsAlice: [],
  fruitsBob: [],
  refreshFruitsInventory: () => {},
  addFruit: () => {},
};

export const FruitContext =
  createContext<FruitContextInterface>(contextDefaultValues);

export const FruitProvider: React.FC = ({ children }) => {
  const [fruitsAlice, setFruitsAlice] = useState<Fruit[]>([]);
  const [fruitsBob, setFruitsBob] = useState<Fruit[]>([]);

  const refreshFruitsInventory = async (identity: string) => {
    let fruits: Fruit[] = [];
    if (identity === "alice") {
      fruits = await FruitService.fetchAllFruits("alice");
      setFruitsAlice(fruits);
    } else {
      fruits = await FruitService.fetchAllFruits("bob");
      setFruitsBob(fruits);
    }
  };

  const addFruit = async (type: string, quantity: number, message: string) => {
    await FruitService.addFruit(type, quantity, message);
  };

  return (
    <FruitContext.Provider
      value={{ fruitsAlice, fruitsBob, refreshFruitsInventory, addFruit }}
    >
      {children}
    </FruitContext.Provider>
  );
};
