import { RouteObject } from "react-router-dom";
import AppLayout from "./ui/AppLayout";
import HomePage from "../features/home/HomePage";
import ReactTsDocsPage from "../features/labs/ReactTsDocsPage";
import BoardsAdminPage from "../features/boards/BoardsAdminPage";

export const routes: RouteObject[] = [
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "labs/react-ts-docs", element: <ReactTsDocsPage /> },
      { path: "boards-admin", element: <BoardsAdminPage /> }
    ]
  }
];
