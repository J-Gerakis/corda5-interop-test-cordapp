import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import { Selection, Alice, Bob } from "./components";

import "./App.scss";

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Selection />} />
        <Route path="/alice" element={<Alice />} />
        <Route path="/bob" element={<Bob />} />
      </Routes>
    </Router>
  );
};

export default App;
