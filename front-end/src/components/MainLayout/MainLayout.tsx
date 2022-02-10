import React from "react";
import {
  Container,
  Row,
  Column,
  Tabs,
  Tab,
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
            <Tabs
              onChange={function (this: any, value) {
                if (this && this.state) {
                  this.changeRadioHandler("selected", value);
                }
              }}
              selected={identity === "alice" ? 0 : 1}
              variant="light"
            >
              <Tab name="fruits">
                <Inventory identity={identity} />
              </Tab>
              <Tab name="offers">
                <div className="bg-black -mt-2">offers</div>
              </Tab>
            </Tabs>
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
