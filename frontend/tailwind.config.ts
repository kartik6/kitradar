import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#15111f",
        panel: "#211a31",
        line: "#49384e",
        signal: "#ff9268",
        cream: "#fff0dc",
      },
    },
  },
  plugins: [],
};

export default config;
