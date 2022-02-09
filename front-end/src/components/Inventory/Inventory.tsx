import React, { useContext, useEffect } from "react";
import { IconButton } from "@r3/r3-tooling-design-system/lib/exports.js";
import ReactTooltip from "react-tooltip";

import { FruitItem, AddFruitModal } from "../../components";
import { FruitContext } from "../../context/fruit";
import { useToggle } from "../../hooks/useToggle";

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
    <div>
      <AddFruitModal open={isModalOpen} toggleModal={toggleModal} />
      <div className="flex items-center justify-between">
        <h5>Fruits</h5>
        <IconButton
          className={identity === "alice" ? "" : "hidden"}
          data-tip
          data-for="addTip"
          icon="Plus"
          size="medium"
          variant="primary"
          onClick={() => {
            toggleModal();
          }}
        ></IconButton>
        <ReactTooltip id="addTip" place="top" effect="solid">
          Add New Fruit
        </ReactTooltip>
      </div>
      <div
        className="mt-4 grid gap-x-8 gap-y-4 py-8 px-8 lg:grid-cols-4 md:grid-cols-3 sm:grid-cols-2 rounded"
        style={{ backgroundColor: "var(--color-medium-light-gray-100)" }}
      >
        {renderFruits()}
      </div>
    </div>
  );
};

export default Inventory;
