import React from "react";
import { Badge } from "@r3/r3-tooling-design-system/lib/exports.js";
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
        className="rounded flex flex-col items-center bg-white cursor-pointer"
        onClick={() => {
          alert();
        }}
      >
        <div className="flex justify-center">
          {renderFruitSvg(fruit.fruitType)}
        </div>
        <p className="font-bold">{fruit.quantity}</p>
        <div className="my-4">
          <Badge variant="green">For Sale</Badge>
        </div>
      </div>
      <ReactTooltip id={fruit.linearId} place="top" effect="solid">
        ID : {fruit.linearId}
      </ReactTooltip>
    </>
  );
};

export default FruitItem;
