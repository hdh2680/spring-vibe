import { useEffect, useState } from "react";
import { useFormState } from "../../shared/hooks/useFormState"

type Board = {
    id:string;
    boardKey?:string;
    name?:string;
    description?:string;
    isEnabled?:boolean;
    isPublicRead?:boolean;
    isPublicWrite?:boolean;
    sortOrder?:number;
}

type Props = {
    mode? : string;
    id? : string | null;
    fnClose : () => void
    fnReload : () => void
}

export default function BoardsAdminDetailModal({mode, id, fnClose, fnReload} : Props){
    const initBoard: Board = {
        id: "0",
        boardKey: "",
        name: "",
        description: "",
        isEnabled: true,
        isPublicRead : true,
        isPublicWrite : true,
        sortOrder : 0
    };

    const { values, setValues, bindValue, setField } = useFormState<Board>(initBoard);

    useEffect(() => {
        if (!id) return;

        fetch(`/api/admin/boards/${id}`)
            .then((r) => r.json())
            .then((data: Board) => {
                setValues((s) => ({ ...s, ...data })); // 받아온 값으로 폼 채우기
            });
    }, [id, setValues]);

    async function fnSave() {
        if(!confirm("저장하시겠습니까?")) return;

        const payload = {
            boardKey: values.boardKey ?? "",
            name: values.name ?? "",
            description: values.description ?? "",
            isEnabled: Boolean(values.isEnabled),
            isPublicRead: Boolean(values.isPublicRead),
            isPublicWrite: Boolean(values.isPublicWrite),
            sortOrder: Number(values.sortOrder ?? 0),
        };

        const isEdit = mode === "edit" && id;
        const url = isEdit ? `/api/admin/boards/${id}` : "/api/admin/boards";
        const method = isEdit ? "PUT" : "POST";

        try {
            const res = await fetch(url, {
                method,
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify(payload),
            });
            if (!res.ok) throw new Error(`${method} failed: ${res.status}`);

            alert("저장이 완료되었습니다");
            fnReload();
            fnClose();
        } catch (e) {
            alert(e instanceof Error ? e.message : String(e));
        }
    }

    async function fnDelete() {
        if(!confirm("삭제하시겠습니까?")) return;

        const url = `/api/admin/boards/${id}`;
        const method = "DELETE";

        try {
            const res = await fetch(url, {
                method,
                headers: { "Content-Type": "application/json" },
                credentials: "include"
            });
            if (!res.ok) throw new Error(`${method} failed: ${res.status}`);

            alert("삭제가 완료되었습니다");
            fnReload();
            fnClose();
        } catch (e) {
            alert(e instanceof Error ? e.message : String(e));
        }
    }

    return (
        <div id="bdModalBackdrop" className="bd-modal-backdrop">
            <div className="bd-modal" role="dialog" aria-modal="true" aria-label="Board upsert">
                <div className="bd-modal-hd">
                    <h3 id="bdModalTitle">게시판 등록</h3>
                    <div style={{display: "inline-flex", gap: 10, alignItems: "center"}}>
                        <button id="bdClose" className="btn-link" type="button" aria-label="닫기" onClick={fnClose}>닫기</button>
                    </div>
                </div>

                <div className="bd-modal-bd">
                    <div className="bd-grid">
                        <label className="field">
                            <div className="label">boardKey</div>
                            <input id="bdKey" className="input" type="text" placeholder="ex) notice" {...bindValue("boardKey")}/>
                            <div className="help">URL/메뉴에서 쓰는 유니크 키</div>
                        </label>

                        <label className="field">
                            <div className="label">이름</div>
                            <input id="bdName" className="input" type="text" placeholder="ex) 공지사항" {...bindValue("name")}/>
                            <div className="help">사용자에게 보여줄 이름</div>
                        </label>

                        <label className="field span-2">
                            <div className="label">설명</div>
                            <input id="bdDesc" className="input" type="text" placeholder="짧은 설명" {...bindValue("description")}/>
                        </label>

                        <label className="field">
                            <div className="label">사용 여부</div>
                            <select id="bdEnabled" className="select" {...bindValue("isEnabled")}>
                                <option value="1">사용</option>
                                <option value="0">중지</option>
                            </select>
                        </label>

                        <label className="field">
                            <div className="label">정렬 순서</div>
                            <input id="bdSort" className="input" type="number" {...bindValue("sortOrder")}/>
                        </label>

                        <label className="field">
                            <div className="label">비회원 읽기</div>
                            <select id="bdPublicRead" className="select" value={values.isPublicRead ? "1" : "0"}
                                    onChange={(e) => setField("isPublicRead", e.target.value === "1")}>
                                <option value="1">허용</option>
                                <option value="0">비허용</option>
                            </select>
                        </label>

                        <label className="field">
                            <div className="label">비회원 쓰기</div>
                            <select id="bdPublicWrite" className="select" value={values.isPublicWrite ? "1" : "0"}
                                    onChange={(e) => setField("isPublicWrite", e.target.value === "1")}>
                                <option value="0">비허용</option>
                                <option value="1">허용</option>
                            </select>
                        </label>
                    </div>
                </div>

                <div className="bd-modal-ft">
                    <div
                        style={{display: "inline-flex", gap: 10, alignItems: "center", justifyContent: "flex-end", width: "100%"}}>
                        <button className="btn-action btn-action--search" type="button" onClick={fnSave}>저장</button>
                        <button className="btn-action btn-action--danger" type="button" onClick={fnDelete}>삭제</button>
                    </div>
                </div>
            </div>
        </div>

    )
}