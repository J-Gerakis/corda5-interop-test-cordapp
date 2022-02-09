import React from "react";

interface Props {
  identity: string;
}

const Token: React.FC<Props> = ({ identity }) => {
  return (
    <div
      className="rounded"
      style={{ backgroundColor: "var(--color-medium-light-gray-100)" }}
    >
      {identity} Token
    </div>
  );
};

export default Token;
