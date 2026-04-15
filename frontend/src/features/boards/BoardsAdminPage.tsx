import { useEffect, useState } from "react";
import Pagination from "../../shared/components/Pagination";
import BoardsAdminDetailModal from "./BoardsAdminDetailModal";

type Boards = {
    id:string;
    boardKey:string;
    name:string;
    description:string;
    isEnabled:boolean;
    isPublicRead:boolean;
    isPublicWrite:boolean;
    sortOrder:number;
    createdAt:string;
    updatedAt:string;
}

type Search = {
    q?:string | "";
    enabled?:string | "";
    page:number | 0;
    size:number | 20;
}

type Pages = {
    total:number;
    totalPages:number;
}

type DetailModal = {
    open : boolean | false; // true|false
    mode? : string | "new" // new|edit
    id? : string | null
}

export default function BoardsAdminPage(){
    const initialSearch:Search = {q: "", enabled: "", page: 0, size: 20};
    const initDetailModal:DetailModal = {open:false}

    const [boards, setBoards] = useState<Boards[]>([]);
    const [search, setSearch] = useState<Search>(initialSearch);
    const [draft, setDraft] = useState<Search>(initialSearch);
    const [pages, setPages] = useState<Pages>({total:0, totalPages:0});
    const [detailModal, setDetailModal] = useState<DetailModal>(initDetailModal);

    useEffect(() => {
        const params = new URLSearchParams();
        params.set("q", search.q ?? "");
        if(search.enabled != null && search.enabled != "all") params.set("enabled", String(search.enabled));
        params.set("page", String(search.page));
        params.set("size", String(search.size));

        fetch(`/api/admin/boards?${params.toString()}`)
            .then((response) => response.json())
            .then(data => {
                setBoards(data.items);
                setPages({total:data.total, totalPages:data.totalPages});

                //Pagination();
            });
    }, [search]);

    function fn_submit(e:any) {
        e.preventDefault();

        setSearch({...draft});
    }

    function fn_reset(e:any) {
        setDraft(initialSearch);
        setSearch(initialSearch);
    }

    return (
        <section className="design-wrap" aria-label="Boards admin design">
            <div className="toolbar">
                <div>
                    <div className="bd-title-row">
                        <h3 style={{margin: 0}}>게시판 관리</h3>
                    </div>
                </div>

                <div>
                    <a className="btn-link" onClick={(e) => setDetailModal((s) => ({...s, open:true, mode:"new"}))}>+ 게시판 등록</a>
                </div>
            </div>

            <section aria-label="Search, table, pagination" style={{marginTop: 12}}>
            <div className="panel panel-inner search-panel">
                    <form className="searchbar" role="search"
                          onSubmit={fn_submit}
                    >
                        <label className="field">
                            <div className="label">검색어</div>
                            <input
                                className="input"
                                type="search"
                                name="q"
                                placeholder="게시판 이름, 키로 검색.."
                                value={draft.q}
                                onChange={(e) => setDraft((d) => ({ ...d, q: e.target.value }))}
                            />
                        </label>

                        <label className="field">
                            <div className="label">사용여부</div>
                            <select className="select" name="enabled"
                                    value={draft.enabled}
                                    onChange={(e) => setDraft((d) => ({ ...d, enabled: e.target.value }))}
                            >
                                <option value="all">전체</option>
                                <option value="1">사용</option>
                                <option value="0">중지</option>
                            </select>
                        </label>

                        <div className="searchbar-actions">
                            <button className="btn-action btn-action--search" type="submit">검색</button>
                            <button className="btn-action btn-action--reset" type="button" onClick={fn_reset}>초기화</button>
                        </div>
                    </form>
                </div>
                <Pagination
                    size={search.size}
                    total = {pages.total}
                    totalPages={pages.totalPages}
                    page={search.page}
                    onChange={(nextPage) =>
                        setSearch((s) => ({ ...s, page: nextPage }))
                    }
                />

                <div className="panel table-wrap" style={{marginTop: 12}}>
                    <table className="table">
                        <thead>
                        <tr>
                            <th style={{width: 80}}>ID</th>
                            <th style={{width: 120}}>키</th>
                            <th style={{width: 220}}>이름</th>
                            <th>설명</th>
                            <th style={{width: 120}}>상태</th>
                            <th style={{width: 80}}>정렬</th>
                            <th style={{width: 180}}>수정일시</th>
                        </tr>
                        </thead>
                        <tbody>
                        {boards.map((board)=>(
                            <tr key={board.id}>
                                <td>{board.id}</td>
                                <td className="muted">{board.boardKey}</td>
                                <td style={{wordBreak: "break-word"}}>
                                    <a className="action" href="#" data-edit="1" onClick={() => setDetailModal({open:true, mode:"edit", id:board.id})}>{board.name}</a>
                                </td>
                                <td className="muted">{board.description}</td>
                                <td><span className="tag"><span className="tag-count">{board.isEnabled ? 'ON' : 'OFF'}</span></span></td>
                                <td className="muted">{board.sortOrder}</td>
                                <td className="muted">{board.updatedAt}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            </section>
            {detailModal.open &&
                <BoardsAdminDetailModal
                    mode={detailModal.mode}
                    id={detailModal.id}
                    fnClose={() => setDetailModal({open:false})}
                    fnReload={() => setSearch((s) => ({...s}))}
                />
            }
        </section>
    )
}