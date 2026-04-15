type PagesProps = {
    size:number; //페이지하나당 데이터 개수
    total:number; //총데이터개수
    page:number; //현재페이지
    totalPages : number; //총페이지 total/size
    onChange?: (page: number) => void; //페이지가 변경됬을때 실행되는 함수
}

export default function Pagination({ size, total, page, totalPages, onChange}: PagesProps){
    function fn_pageMove(e: React.MouseEvent<HTMLButtonElement>) {
        const last = Math.max(0, totalPages - 1);
        const nextPage =
            e.currentTarget.id === "pagePrevBtn" ? Math.max(0, page - 1) :
                e.currentTarget.id === "pageNextBtn" ? Math.min(last, page + 1) :
                    page;

        onChange?.(nextPage);
    }

    return (
        <div className="pager" style={{marginTop: 12}}>
            <div className="muted">
                페이지: <b>{page+1}</b> / <b>{totalPages}</b>
                <span style={{opacity:.5}}> | </span>
                페이지당: <b>{size}</b>
            </div>
            <div className="pager-actions">
                <button type="button" id="pagePrevBtn" className="btn-link" onClick={fn_pageMove}>이전</button>
                <button type="button" id="pageNextBtn" className="btn-link" onClick={fn_pageMove}>다음</button>
            </div>
        </div>
    )
}