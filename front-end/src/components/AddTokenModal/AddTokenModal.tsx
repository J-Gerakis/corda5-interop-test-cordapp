import React, { useState } from "react";
import { Button, Select, Option, Modal } from "@r3/r3-tooling-design-system";

interface Props {
  open: boolean;
  toggleModal: () => void;
}

const AddTokenModal: React.FC<Props> = ({ open, toggleModal }) => {
  const [quantity, setQuantity] = useState("");

  const handleCancel = () => {
    setQuantity("");
    toggleModal();
  };

  const handleSubmit = async () => {
    setQuantity("");
    toggleModal();
  };

  return (
    <div>
      <Modal
        onClose={handleCancel}
        size="small"
        title="Issue New Token"
        withBackdrop
        open={open}
      >
        <div className="flex flex-col gap-4 my-4">
          <div className="flex flex-row gap-4">
            <Select
              label="Quantity"
              value={quantity}
              onChange={(event): void => {
                setQuantity(event.target.value);
              }}
            >
              <Option value="" disabled></Option>
              <Option value="10">10</Option>
              <Option value="20">20</Option>
              <Option value="50">50</Option>
              <Option value="100">100</Option>
            </Select>
          </div>
        </div>

        <div className="flex justify-end gap-4 my-4">
          <Button
            size="small"
            variant="secondary"
            onClick={() => handleCancel()}
          >
            Cancel
          </Button>
          <Button size="small" variant="primary" onClick={() => handleSubmit()}>
            Save
          </Button>
        </div>
      </Modal>
    </div>
  );
};

export default AddTokenModal;
