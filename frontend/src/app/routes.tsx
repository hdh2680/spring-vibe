import { RouteObject } from "react-router-dom";
import AppLayout from "./ui/AppLayout";
import HomePage from "../features/home/HomePage";
import ReactTsDocsPage from "../features/labs/ReactTsDocsPage";

export const routes: RouteObject[] = [
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "labs/react-ts-docs", element: <ReactTsDocsPage /> }
    ]
  }
];
