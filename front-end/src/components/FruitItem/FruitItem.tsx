import React from "react";
import { Badge, Button } from "@r3/r3-tooling-design-system/lib/exports.js";
import ReactTooltip from "react-tooltip";

import { Fruit } from "../../models/models";
import { ReactComponent as Apple } from "../../assets/fruits/apple.svg";
import { ReactComponent as Banana } from "../../assets/fruits/banana.svg";
import { ReactComponent as Watermelon } from "../../assets/fruits/watermelon.svg";

interface Props {
  fruit: Fruit;
}

const FruitItem: React.FC<Props> = ({ fruit }) => {
  const renderFruitSvg = (fruitType: string) => {
    switch (fruitType.toLowerCase()) {
      case "watermelon":
        return <Watermelon className="w-20" />;
      case "apple":
        return <Apple className="w-20" />;
      case "banana":
        return <Banana className="w-20" />;
      default:
        return null;
    }
  };

  return (
    <>
      <div
        data-tip
        data-for={fruit.linearId}
        className="rounded flex flex-col items-center bg-white"
      >
        <div className="flex justify-center">
          {renderFruitSvg(fruit.fruitType)}
        </div>
        <p className="font-bold">{fruit.quantity}</p>
        <div className="my-4">
          <Badge variant={fruit.status === "available" ? "green" : "red"}>
            {fruit.status}
          </Badge>
        </div>
        <div
          className="w-full h-full rounded-b py-4"
          style={{ backgroundColor: "var(--color-medium-light-gray-200)" }}
        >
          <div className="flex w-full h-full items-center justify-center">
            {fruit.status === "available" ? (
              <Button size="small" variant="secondary">
                Invite to Buy
              </Button>
            ) : (
              <p>No Action</p>
            )}
          </div>
        </div>
      </div>
      <ReactTooltip id={fruit.linearId} place="top" effect="solid">
        ID : {fruit.linearId}
      </ReactTooltip>
    </>
  );
};

export default FruitItem;
