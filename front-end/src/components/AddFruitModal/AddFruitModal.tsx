import React, { useState, useContext } from "react";
import {
  Button,
  Select,
  Option,
  Modal,
  TextInput,
  NotificationService,
  Snackbar,
} from "@r3/r3-tooling-design-system";

import { FruitContext } from "../../context/fruit";

interface Props {
  open: boolean;
  toggleModal: () => void;
}

const AddFruitModal: React.FC<Props> = ({ open, toggleModal }) => {
  const [quantity, setQuantity] = useState("");
  const [type, setType] = useState("");

  const { refreshFruitsInventory, addFruit } = useContext(FruitContext);

  const handleCancel = () => {
    setQuantity("");
    setType("");
    toggleModal();
  };

  const handleSubmit = async () => {
    try {
      await addFruit(type, parseInt(quantity), "");
      NotificationService.addNotification(
        <Snackbar variant="success">Success: Added New Fruit.</Snackbar>
      );
    } catch (error) {
      console.error(error);
    }

    setTimeout(() => {
      refreshFruitsInventory("alice");
    }, 1500);

    setQuantity("");
    setType("");
    toggleModal();
  };

  return (
    <div>
      <Modal
        onClose={handleCancel}
        size="small"
        title="Add Fruit"
        withBackdrop
        open={open}
      >
        <div className="flex flex-col gap-4 my-4">
          <div className="flex flex-row gap-4">
            <Select
              label="Type"
              value={type}
              onChange={(event): void => {
                setType(event.target.value);
              }}
            >
              <Option value="" disabled></Option>
              <Option value="WATERMELON">Watermelon</Option>
              <Option value="BANANA">Banana</Option>
              <Option value="APPLE">Apple</Option>
            </Select>
            <TextInput
              label="Quantity"
              onChange={(event): void => {
                setQuantity(event.target.value);
              }}
              value={quantity}
              type="number"
            />
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

export default AddFruitModal;
