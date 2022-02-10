import React from "react";
import { IconButton, Badge } from "@r3/r3-tooling-design-system/lib/exports.js";
import ReactTooltip from "react-tooltip";

import { useToggle } from "../../hooks/useToggle";
import { AddTokenModal } from "../../components";
interface Props {
  identity: string;
}

const Token: React.FC<Props> = ({ identity }) => {
  const [isModalOpen, toggleModal] = useToggle();

  return (
    <div className="mb-8">
      <AddTokenModal open={isModalOpen} toggleModal={toggleModal} />
      <div className="flex items-baseline justify-between">
        <h4 className="mb-2">Tokens</h4>
        <IconButton
          className={identity === "bob" ? "" : "hidden"}
          data-tip
          data-for="addTip"
          icon="Plus"
          size="medium"
          variant="primary"
          onClick={() => {
            toggleModal();
          }}
        />
        <ReactTooltip id="addTip" place="top" effect="solid">
          Issue New Token
        </ReactTooltip>
      </div>
      <div
        className="mt-4 flex flex-col rounded"
        style={{ backgroundColor: "var(--color-medium-light-gray-100)" }}
      >
        <div
          className="grid grid-cols-2 px-8 py-4 rounded-t"
          style={{ backgroundColor: "var(--color-medium-light-gray-200)" }}
        >
          <h6>Encumbered</h6>
          <h6>Total</h6>
        </div>
        <div className="grid grid-cols-2 p-8">
          <div>
            <Badge variant="yellow">10</Badge>
          </div>
          <div>
            <Badge variant="green">100</Badge>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Token;
