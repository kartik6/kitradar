import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#101115",
        panel: "#181a20",
        line: "#2b2e37",
        signal: "#d8ff3e",
      },
    },
  },
  plugins: [],
};

export default config;
