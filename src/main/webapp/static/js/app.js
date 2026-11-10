(function () {
    "use strict";

    const { createApp } = Vue;

    const CARDS = [
        { perm: "user:query", title: "用户信息查询", route: "users", icon: "👤", desc: "按关键字查询用户（只读）" },
        { perm: "loan:query", title: "借阅记录查询", route: "loan-query", icon: "🔎", desc: "关键字 / 超期 / 日期范围" },
        { perm: "loan:stats", title: "借阅数据统计", route: "stats", icon: "📊", desc: "按图书类型、按专业" },
        { perm: "loan:borrow", title: "借书", route: "borrow", icon: "📖", desc: "选择可借图书" },
        { perm: "loan:return", title: "还书", route: "return", icon: "↩️", desc: "归还自己的未还图书" },
        { perm: "recommend:review", title: "审阅推荐图书", route: "review", icon: "✅", desc: "通过 / 驳回教师推荐" },
        { perm: "book:add", title: "添加书籍", route: "books", icon: "➕", desc: "自动按库存生成副本" },
        { perm: "book:read", title: "查看所有书籍", route: "books", icon: "📚", desc: "查询、编辑、废弃、副本" },
        { perm: "book:discard", title: "废弃图书", route: "books", icon: "🗑️", desc: "无外借副本时可废弃" },
        { perm: "book:edit", title: "编辑图书", route: "books", icon: "✏️", desc: "只改 6 个字段" },
        { perm: "copy:edit", title: "图书副本编辑", route: "books", icon: "🧩", desc: "新增 / 删除副本" },
        {
            perm: "loan:self", title: "个人借阅查询", route: "my-loans", icon: "📋",
            desc: "全部 / 未还 / 日期范围", roles: ["操作员", "学生", "教师"]
        },
        { perm: "recommend:add", title: "推荐图书", route: "recommend", icon: "💡", desc: "有馆藏直接返回，否则提交审核" }
    ];

    function emptyBook() {
        return {
            title: "", author: "", type: "", publisher: "",
            days: 7, price: 0, stock: 0, introduction: ""
        };
    }

    function emptyRecommend() {
        return { bookName: "", author: "", publisher: "", price: 0 };
    }

    createApp({
        data() {
            return {
                user: { id: "", name: "", role: "", permissions: [] },
                view: "home",
                toast: "",
                toastTimer: null,

                users: { keyword: "", list: [], searched: false, detail: null },

                books: {
                    keyword: "", list: [], saving: false,
                    showAdd: false, addForm: emptyBook(),
                    showEdit: false, editForm: null,
                    showCopies: false, copyBook: null, copies: []
                },

                borrow: { available: [], active: [] },
                returnData: { list: [] },
                myLoans: { scope: "all", start: "", end: "", list: [] },
                loanQuery: { mode: "all", keyword: "", start: "", end: "", list: [] },
                stats: { byType: [], byMajor: [], maxType: 1, maxMajor: 1 },
                recommend: { form: emptyRecommend(), existing: [], mine: [], loading: false },
                review: {
                    list: [], saving: false,
                    approve: null,
                    approveForm: { type: "", days: 30, stock: 1, introduction: "" },
                    reject: null,
                    rejectReason: ""
                }
            };
        },
        computed: {
            initial() {
                return this.user.name ? this.user.name.substring(0, 1).toUpperCase() : "?";
            },
            cards() {
                return CARDS.filter(card =>
                    this.user.permissions.includes(card.perm) &&
                    (!card.roles || card.roles.includes(this.user.role))
                );
            }
        },
        mounted() {
            window.addEventListener("api-error", event => this.showToast(event.detail));
            window.addEventListener("hashchange", () => this.syncView());
            this.loadMe().then(() => {
                if (!location.hash) {
                    location.hash = "#/home";
                } else {
                    this.syncView();
                }
            });
        },
        methods: {
            hasPerm(permission) {
                return this.user.permissions.includes(permission);
            },
            showToast(message) {
                if (!message) return;
                this.toast = message;
                if (this.toastTimer) clearTimeout(this.toastTimer);
                this.toastTimer = setTimeout(() => { this.toast = ""; }, 2800);
            },
            go(route) {
                location.hash = "#/" + route;
            },
            async loadMe() {
                try {
                    const data = await api.get("auth/me");
                    this.user = data.user || { id: "", name: "", role: "", permissions: [] };
                    this.user.permissions = data.permissions || [];
                } catch (e) {
                    // 401 已由 api.js 跳转
                }
            },
            async logout() {
                try { await api.post("auth/logout"); } catch (e) { /* ignore */ }
                location.href = "login.html";
            },
            syncView() {
                const route = (location.hash || "#/home").replace(/^#\//, "") || "home";
                const allowed = {
                    home: true,
                    users: this.hasPerm("user:query"),
                    books: this.hasPerm("book:read"),
                    borrow: this.hasPerm("loan:borrow"),
                    "return": this.hasPerm("loan:return"),
                    "my-loans": this.hasPerm("loan:self"),
                    "loan-query": this.hasPerm("loan:query"),
                    stats: this.hasPerm("loan:stats"),
                    recommend: this.hasPerm("recommend:add"),
                    review: this.hasPerm("recommend:review")
                };
                if (!allowed[route]) {
                    this.showToast("没有访问该功能的权限");
                    location.hash = "#/home";
                    return;
                }
                this.view = route;
                this.onViewEnter(route);
            },
            onViewEnter(route) {
                if (route === "books") this.loadBooks();
                else if (route === "borrow") this.loadBorrow();
                else if (route === "return") this.loadReturn();
                else if (route === "my-loans") this.loadMyLoans();
                else if (route === "loan-query") this.loadLoanQuery();
                else if (route === "stats") this.loadStats();
                else if (route === "recommend") this.loadRecommend();
                else if (route === "review") this.loadReview();
            },

            async searchUsers() {
                if (!this.users.keyword) {
                    this.users.list = [];
                    this.users.searched = false;
                    this.showToast("请输入关键字");
                    return;
                }
                try {
                    this.users.list = await api.get("users", { params: { keyword: this.users.keyword } });
                    this.users.searched = true;
                } catch (e) { /* toast */ }
            },
            async openUser(id) {
                try { this.users.detail = await api.get("users/" + id); } catch (e) { /* toast */ }
            },

            async loadBooks() {
                try {
                    this.books.list = await api.get("books", { params: { keyword: this.books.keyword } });
                } catch (e) { /* toast */ }
            },
            openAdd() {
                this.books.addForm = emptyBook();
                this.books.showAdd = true;
            },
            async saveBook() {
                this.books.saving = true;
                try {
                    await api.post("books", this.books.addForm);
                    this.books.addForm = emptyBook();
                    await this.loadBooks();
                    this.showToast("添加成功，可继续添加");
                } catch (e) { /* toast */ } finally {
                    this.books.saving = false;
                }
            },
            openEdit(book) {
                this.books.editForm = {
                    id: book.id, title: book.title, author: book.author,
                    type: book.type, publisher: book.publisher,
                    days: book.days, price: book.price
                };
                this.books.showEdit = true;
            },
            async saveEdit() {
                this.books.saving = true;
                try {
                    await api.put("books/" + this.books.editForm.id, this.books.editForm);
                    this.books.showEdit = false;
                    await this.loadBooks();
                    this.showToast("保存成功");
                } catch (e) { /* toast */ } finally {
                    this.books.saving = false;
                }
            },
            async discardBook(book) {
                if (book.borrowedCopies > 0) {
                    this.showToast("存在未归还副本，不能废弃");
                    return;
                }
                if (!confirm("确定废弃《" + book.title + "》吗？废弃后不可借阅，但借阅记录会保留。")) return;
                try {
                    await api.post("books/" + book.id + "/discard");
                    await this.loadBooks();
                    this.showToast("已废弃");
                } catch (e) { /* toast */ }
            },
            async openCopies(book) {
                this.books.copyBook = book;
                this.books.showCopies = true;
                await this.loadCopies(book.id);
            },
            async loadCopies(bookId) {
                try {
                    this.books.copies = await api.get("books/" + bookId + "/copies");
                } catch (e) { /* toast */ }
            },
            async addCopy() {
                if (!this.books.copyBook) return;
                this.books.saving = true;
                try {
                    await api.post("books/" + this.books.copyBook.id + "/copies");
                    await this.loadCopies(this.books.copyBook.id);
                    await this.loadBooks();
                    this.showToast("新增副本成功，库存 +1");
                } catch (e) { /* toast */ } finally {
                    this.books.saving = false;
                }
            },
            async deleteCopy(copy) {
                if (copy.isBorrowed) return;
                if (!confirm("确定删除该副本吗？删除后库存 -1。")) return;
                this.books.saving = true;
                try {
                    await api.delete("copies/" + copy.id);
                    await this.loadCopies(this.books.copyBook.id);
                    await this.loadBooks();
                    this.showToast("删除副本成功，库存 -1");
                } catch (e) { /* toast */ } finally {
                    this.books.saving = false;
                }
            },

            async loadBorrow() {
                try {
                    const result = await Promise.all([
                        api.get("loans/mine", { params: { scope: "unreturned" } }),
                        api.get("books/borrowable")
                    ]);
                    this.borrow.active = result[0];
                    this.borrow.available = result[1];
                } catch (e) { /* toast */ }
            },
            async borrowBook(book) {
                if (!confirm("确定借阅《" + book.title + "》吗？")) return;
                try {
                    const data = await api.post("loans", { bookId: book.id });
                    this.showToast("借阅成功，应还日期：" + data.dueDate);
                    await this.loadBorrow();
                } catch (e) { /* toast */ }
            },
            async loadReturn() {
                try {
                    this.returnData.list = await api.get("loans/mine", { params: { scope: "unreturned" } });
                } catch (e) { /* toast */ }
            },
            async returnBook(loan) {
                if (!confirm("确定归还《" + loan.bookTitle + "》吗？")) return;
                try {
                    await api.post("loans/return", { copyId: loan.copyId });
                    await this.loadReturn();
                    this.showToast("归还成功");
                } catch (e) { /* toast */ }
            },
            async loadMyLoans() {
                if (this.myLoans.start && this.myLoans.end && this.myLoans.start > this.myLoans.end) {
                    this.showToast("开始日期不能晚于结束日期");
                    return;
                }
                try {
                    this.myLoans.list = await api.get("loans/mine", {
                        params: {
                            scope: this.myLoans.scope,
                            start: this.myLoans.start,
                            end: this.myLoans.end
                        }
                    });
                } catch (e) { /* toast */ }
            },
            setLoanMode(mode) {
                this.loanQuery.mode = mode;
                this.loadLoanQuery();
            },
            async loadLoanQuery() {
                if (this.loanQuery.mode === "range") {
                    if (!this.loanQuery.start || !this.loanQuery.end) {
                        this.showToast("请选择开始和结束日期");
                        return;
                    }
                    if (this.loanQuery.start > this.loanQuery.end) {
                        this.showToast("开始日期不能晚于结束日期");
                        return;
                    }
                }
                const params = {
                    mode: this.loanQuery.mode,
                    keyword: this.loanQuery.keyword,
                    start: this.loanQuery.mode === "range" ? this.loanQuery.start : "",
                    end: this.loanQuery.mode === "range" ? this.loanQuery.end : ""
                };
                try {
                    this.loanQuery.list = await api.get("loans", { params });
                } catch (e) { /* toast */ }
            },
            async loadStats() {
                try {
                    const result = await Promise.all([
                        api.get("stats/by-type"),
                        api.get("stats/by-major")
                    ]);
                    this.stats.byType = result[0];
                    this.stats.byMajor = result[1];
                    this.stats.maxType = Math.max(1, ...result[0].map(item => item.total));
                    this.stats.maxMajor = Math.max(1, ...result[1].map(item => item.total));
                } catch (e) { /* toast */ }
            },
            barWidth(total, max) {
                return Math.max(0, Math.min(100, Math.round(total * 100 / max))) + "%";
            },

            async loadRecommend() {
                try {
                    this.recommend.mine = await api.get("recommendations/mine");
                } catch (e) { /* toast */ }
            },
            async submitRecommend() {
                this.recommend.loading = true;
                try {
                    const result = await api.post("recommendations", this.recommend.form);
                    if (result.submitted) {
                        this.recommend.existing = [];
                        this.recommend.form = emptyRecommend();
                        await this.loadRecommend();
                        this.showToast("已提交到图书审核");
                    } else {
                        this.recommend.existing = result.books || [];
                        this.showToast("馆藏已有相关图书，未重复提交");
                    }
                } catch (e) { /* toast */ } finally {
                    this.recommend.loading = false;
                }
            },
            async loadReview() {
                try {
                    this.review.list = await api.get("recommendations");
                } catch (e) { /* toast */ }
            },
            openApprove(item) {
                this.review.approve = item;
                this.review.approveForm = { type: "", days: 30, stock: 1, introduction: "" };
            },
            async approve() {
                this.review.saving = true;
                try {
                    await api.post("recommendations/" + this.review.approve.id + "/approve", this.review.approveForm);
                    this.review.approve = null;
                    await this.loadReview();
                    this.showToast("已通过，图书已加入列表");
                } catch (e) { /* toast */ } finally {
                    this.review.saving = false;
                }
            },
            openReject(item) {
                this.review.reject = item;
                this.review.rejectReason = "";
            },
            async reject() {
                if (!this.review.rejectReason) {
                    this.showToast("请填写驳回理由");
                    return;
                }
                this.review.saving = true;
                try {
                    await api.post("recommendations/" + this.review.reject.id + "/reject",
                        { reason: this.review.rejectReason });
                    this.review.reject = null;
                    await this.loadReview();
                    this.showToast("已驳回");
                } catch (e) { /* toast */ } finally {
                    this.review.saving = false;
                }
            },

            statusClass(status) {
                if (status === "正常") return "normal";
                if (status === "已废弃") return "discarded";
                return "";
            },
            loanStatusClass(loan) {
                if (loan.overdue || loan.status === "超期") return "overdue";
                if (loan.status === "未还") return "unreturned";
                if (loan.status === "已还") return "returned";
                return "";
            },
            recommendStatusClass(status) {
                if (status === "待审核") return "pending";
                if (status === "已驳回") return "rejected";
                if (status === "已通过") return "normal";
                return "";
            },
            shortId(id) {
                return id ? id.substring(0, 8) : "—";
            }
        }
    }).mount("#app");
})();
