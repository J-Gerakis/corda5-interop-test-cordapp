import React from "react";
import {
  DashboardItem,
  TopNavBar,
} from "@r3/r3-tooling-design-system/lib/exports.js";
import { Link } from "react-router-dom";

import { ReactComponent as Logo } from "../../assets/r3-logo.svg";

const Selection: React.FC = () => {
  return (
    <>
      <TopNavBar
        logo={
          <a href="/">
            <Logo width={30} />
          </a>
        }
        title="Atomic Swap Recovery Demo"
      ></TopNavBar>
      <div className="content-wrapper">
        <div className="flex flex-row items-center justify-center space-x-10 my-32">
          <div className="h-48 w-48">
            <Link to="/alice">
              <DashboardItem icon="AstronautFishing" withBackground>
                Alice
              </DashboardItem>
            </Link>
          </div>
          <div className="h-48 w-48">
            <Link to="/bob">
              <DashboardItem icon="AstronautHello" withBackground>
                Bob
              </DashboardItem>
            </Link>
          </div>
        </div>
      </div>
      <div className="footer-wrapper flex justify-center bottom-0 absolute mb-6 w-full">
        {"Copyright ©R3 " + new Date().getFullYear()}
      </div>
    </>
  );
};

export default Selection;
