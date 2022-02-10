import React, { useContext, useEffect } from "react";
import { Button } from "@r3/r3-tooling-design-system/lib/exports.js";

import { FruitItem, AddFruitModal } from "../../components";
import { FruitContext } from "../../context/fruit";
import { useToggle } from "../../hooks/useToggle";
import { Fruit } from "../../models/models";

interface Props {
  identity: string;
}

const Inventory: React.FC<Props> = ({ identity }) => {
  const [isModalOpen, toggleModal] = useToggle();

  const { refreshFruitsInventory, fruitsAlice, fruitsBob } =
    useContext(FruitContext);

  useEffect(() => {
    refreshFruitsInventory(identity);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [identity]);

  const renderFruits = () => {
    const fakedFruit: Fruit = {
      fruitType: "WATERMELON",
      quantity: 12,
      message: "",
      ownerName: "alice",
      timestamp: "time",
      linearId: "linearId",
      status: "sold",
    };
    fruitsAlice.push(fakedFruit);

    if (identity === "alice") {
      if (fruitsAlice.length === 0) {
        return <p className="text-center">No fruits yet.</p>;
      }
      return fruitsAlice.map((fruit) => (
        <FruitItem key={fruit.linearId} fruit={fruit} />
      ));
    } else {
      if (fruitsBob.length === 0) {
        return <p className="text-center">No fruits yet.</p>;
      }
      return fruitsBob.map((fruit) => (
        <FruitItem key={fruit.linearId} fruit={fruit} />
      ));
    }
  };

  return (
    <div
      className="-mt-2 rounded-b rounded-tr mb-4"
      style={{ backgroundColor: "var(--color-medium-light-gray-100)" }}
    >
      <AddFruitModal open={isModalOpen} toggleModal={toggleModal} />
      <div className="flex justify-start pl-8 pt-4">
        <Button
          className={identity === "alice" ? "" : "hidden"}
          size="small"
          variant="primary"
          onClick={() => {
            toggleModal();
          }}
        >
          + New Fruit
        </Button>
      </div>
      <div className="mt-4 grid gap-x-8 gap-y-8 pt-2 pb-8 px-8 lg:grid-cols-4 md:grid-cols-3 sm:grid-cols-2 rounded">
        {renderFruits()}
      </div>
    </div>
  );
};

export default Inventory;
