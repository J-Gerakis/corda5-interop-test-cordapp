import { useCallback, useState, useEffect } from "react";

export const useToggle = (initialState: boolean = false): [boolean, any] => {
  // Initialize the state
  const [state, setState] = useState<boolean>(initialState);
  // Define and memorize toggler function in case we pass down the comopnent,
  // This function change the boolean value to it's opposite value
  const toggle = useCallback((): void => setState((state) => !state), []);

  useEffect(() => {
    state && document.body.classList.add("overflow-hidden");
    return (): void => {
      document.body.classList.remove("overflow-hidden");
    };
  }, [state]);

  return [state, toggle];
};
