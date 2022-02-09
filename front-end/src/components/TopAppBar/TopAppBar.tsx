import React from "react";
import {
  TopNavBar,
  IconCustom,
} from "@r3/r3-tooling-design-system/lib/exports.js";
import { Link } from "react-router-dom";

import { ReactComponent as Logo } from "../../assets/r3-logo.svg";

interface Props {
  identity: string;
}

const TopAppBar: React.FC<Props> = ({ identity }) => {
  return (
    <TopNavBar
      title="Atomic Swap Recovery Demo - Fruit Trading"
      center={
        <React.Fragment>
          <a
            href="https://www.r3.com/contact/"
            rel="noopener noreferrer"
            target="_blank"
          >
            contact us
          </a>
        </React.Fragment>
      }
      centerPos="end"
      logo={
        <a href="/">
          <Logo width={30} />
        </a>
      }
    >
      <Link to="/">
        <span className="flex items-center">
          {identity}
          <IconCustom className="h-5 ml-1" icon="ExitToApp" />
        </span>
      </Link>
    </TopNavBar>
  );
};

export default TopAppBar;
