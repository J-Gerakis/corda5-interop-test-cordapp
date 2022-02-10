import React from "react";
import {
  Container,
  Row,
  Column,
} from "@r3/r3-tooling-design-system/lib/exports.js";

import { TopAppBar, Inventory, Token } from "../../components";

interface Props {
  identity: string;
}

const MainLayout: React.FC<Props> = ({ identity }) => {
  return (
    <div className="h-screen">
      <TopAppBar identity={identity} />
      <Container className="mt-10">
        <Row>
          <Column lg={8} md={12}>
            <Inventory identity={identity} />
          </Column>
          <Column lg={4} md={12}>
            <Token identity={identity} />
          </Column>
        </Row>
      </Container>
    </div>
  );
};

export default MainLayout;
