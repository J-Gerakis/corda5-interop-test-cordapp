import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { FruitProvider } from "./context/fruit";

import { Selection, MainLayout } from "./components";

const App: React.FC = () => {
  return (
    <FruitProvider>
      <Router>
        <Routes>
          <Route path="/" element={<Selection />} />
          <Route path="/alice" element={<MainLayout identity="alice" />} />
          <Route path="/bob" element={<MainLayout identity="bob" />} />
        </Routes>
      </Router>
    </FruitProvider>
  );
};

export default App;
