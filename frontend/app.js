'use strict';

/* ===================================================================
   Markdown Editor – Frontend App
   =================================================================== */

const API_BASE = window.location.origin;

// --- DOM refs ---
const editor        = document.getElementById('editor');
const preview       = document.getElementById('preview');
const filenameInput = document.getElementById('filename-input');
const charCount     = document.getElementById('char-count');
const wordCount     = document.getElementById('word-count');
const lineCount     = document.getElementById('line-count');
const editorContainer = document.getElementById('editor-container');
const fileModal     = document.getElementById('file-modal');
const fileList      = document.getElementById('file-list');
const fileListEmpty = document.getElementById('file-list-empty');
const toast         = document.getElementById('toast');

// --- Configure marked ---
if (typeof marked !== 'undefined') {
  marked.setOptions({
    gfm: true,
    breaks: true,
    headerIds: true,
  });
}

// --- Simple undo/redo history stack ---
const HISTORY_LIMIT = 200;
let history = [''];
let historyIndex = 0;
let historyPaused = false;

function pushHistory(value) {
  if (historyPaused) return;
  // Discard any redo tail
  if (historyIndex < history.length - 1) {
    history = history.slice(0, historyIndex + 1);
  }
  if (history[historyIndex] === value) return;
  history.push(value);
  if (history.length > HISTORY_LIMIT) history.shift();
  historyIndex = history.length - 1;
}

function applyHistory(value) {
  historyPaused = true;
  const start = editor.selectionStart;
  const end   = editor.selectionEnd;
  editor.value = value;
  editor.setSelectionRange(start, end);
  historyPaused = false;
  renderPreview();
  updateStatus();
}

function undo() {
  if (historyIndex > 0) {
    historyIndex--;
    applyHistory(history[historyIndex]);
  }
}

function redo() {
  if (historyIndex < history.length - 1) {
    historyIndex++;
    applyHistory(history[historyIndex]);
  }
}

// --- Render preview ---
function renderPreview() {
  if (typeof marked === 'undefined') return;
  const html = marked.parse(editor.value || '');
  preview.innerHTML = html;
  // Highlight code blocks if hljs is available
  if (typeof hljs !== 'undefined') {
    preview.querySelectorAll('pre code').forEach((block) => {
      hljs.highlightElement(block);
    });
  }
}

// --- Status bar ---
function updateStatus() {
  const text = editor.value;
  charCount.textContent = `${text.length} 字符`;
  const words = text.trim() ? text.trim().split(/\s+/).length : 0;
  wordCount.textContent = `${words} 词`;
  lineCount.textContent = `${text.split('\n').length} 行`;
}

// --- Toast notifications ---
let toastTimer = null;
function showToast(msg, type = 'success') {
  toast.textContent = msg;
  toast.className = `toast ${type}`;
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.classList.add('hidden'); }, 2500);
}

// --- API helpers ---
async function apiFetch(url, options = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  return data;
}

// --- Save ---
async function saveFile() {
  const name = filenameInput.value.trim() || 'untitled';
  filenameInput.value = name;
  try {
    await apiFetch(`${API_BASE}/api/files`, {
      method: 'POST',
      body: JSON.stringify({ name, content: editor.value }),
    });
    showToast(`✅ 已保存：${name}.md`);
    document.title = `${name} – Markdown 编辑器`;
  } catch (err) {
    showToast(`❌ 保存失败：${err.message}`, 'error');
  }
}

// --- Load ---
async function loadFile(name) {
  try {
    const data = await apiFetch(`${API_BASE}/api/files/${encodeURIComponent(name)}`);
    editor.value = data.content;
    filenameInput.value = data.name.replace(/\.md$/, '');
    renderPreview();
    updateStatus();
    document.title = `${filenameInput.value} – Markdown 编辑器`;
    showToast(`📂 已打开：${data.name}`);
    closeModal();
  } catch (err) {
    showToast(`❌ 打开失败：${err.message}`, 'error');
  }
}

// --- Delete ---
async function deleteFile(name) {
  if (!confirm(`确定要删除文件 "${name}" 吗？`)) return;
  try {
    await apiFetch(`${API_BASE}/api/files/${encodeURIComponent(name)}`, { method: 'DELETE' });
    showToast(`🗑️ 已删除：${name}`);
    openFileModal(); // refresh list
  } catch (err) {
    showToast(`❌ 删除失败：${err.message}`, 'error');
  }
}

// --- New file ---
function newFile() {
  if (editor.value && !confirm('当前内容未保存，确定新建？')) return;
  editor.value = '';
  filenameInput.value = 'untitled';
  renderPreview();
  updateStatus();
  document.title = 'Markdown 编辑器';
  editor.focus();
}

// --- File modal ---
async function openFileModal() {
  fileModal.classList.remove('hidden');
  fileList.innerHTML = '';
  fileListEmpty.classList.add('hidden');
  try {
    const data = await apiFetch(`${API_BASE}/api/files`);
    if (!data.files || data.files.length === 0) {
      fileListEmpty.classList.remove('hidden');
      return;
    }
    data.files.forEach((f) => {
      const li = document.createElement('li');
      li.className = 'file-item';

      const info = document.createElement('div');
      info.className = 'file-item-info';

      const nameEl = document.createElement('div');
      nameEl.className = 'file-item-name';
      nameEl.textContent = f.name;

      const metaEl = document.createElement('div');
      metaEl.className = 'file-item-meta';
      const date = new Date(f.updatedAt);
      metaEl.textContent = `${date.toLocaleString('zh-CN')}  ·  ${(f.size / 1024).toFixed(1)} KB`;

      info.appendChild(nameEl);
      info.appendChild(metaEl);

      const delBtn = document.createElement('button');
      delBtn.className = 'file-item-del';
      delBtn.title = '删除';
      delBtn.textContent = '🗑';
      delBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        deleteFile(f.name);
      });

      li.appendChild(info);
      li.appendChild(delBtn);
      li.addEventListener('click', () => loadFile(f.name));
      fileList.appendChild(li);
    });
  } catch (err) {
    fileListEmpty.textContent = `加载失败：${err.message}`;
    fileListEmpty.classList.remove('hidden');
  }
}

function closeModal() {
  fileModal.classList.add('hidden');
}

// --- Toolbar actions ---
function wrapSelection(before, after = before) {
  const start = editor.selectionStart;
  const end   = editor.selectionEnd;
  const sel   = editor.value.substring(start, end);
  const replacement = `${before}${sel}${after}`;
  editor.setRangeText(replacement, start, end, 'select');
  editor.focus();
  renderPreview();
  pushHistory(editor.value);
}

function insertAtLineStart(prefix) {
  const start = editor.selectionStart;
  const lineStart = editor.value.lastIndexOf('\n', start - 1) + 1;
  editor.setRangeText(prefix, lineStart, lineStart, 'end');
  editor.focus();
  renderPreview();
  pushHistory(editor.value);
}

const TOOLBAR_ACTIONS = {
  bold:          () => wrapSelection('**'),
  italic:        () => wrapSelection('_'),
  strikethrough: () => wrapSelection('~~'),
  h1:            () => insertAtLineStart('# '),
  h2:            () => insertAtLineStart('## '),
  h3:            () => insertAtLineStart('### '),
  ul:            () => insertAtLineStart('- '),
  ol:            () => insertAtLineStart('1. '),
  task:          () => insertAtLineStart('- [ ] '),
  quote:         () => insertAtLineStart('> '),
  hr:            () => { wrapSelection('\n---\n', ''); },
  code:          () => wrapSelection('`'),
  codeblock:     () => wrapSelection('```\n', '\n```'),
  link: () => {
    const sel = editor.value.substring(editor.selectionStart, editor.selectionEnd) || '链接文字';
    const url = prompt('请输入链接地址：', 'https://');
    if (!url) return;
    // Basic URL validation: must start with a safe scheme
    if (!/^https?:\/\//i.test(url)) { showToast('❌ 请输入以 http:// 或 https:// 开头的链接', 'error'); return; }
    wrapSelection(`[${sel}](`, `${url})`);
  },
  image: () => {
    const alt = editor.value.substring(editor.selectionStart, editor.selectionEnd) || '图片描述';
    const url = prompt('请输入图片地址：', 'https://');
    if (!url) return;
    if (!/^https?:\/\//i.test(url)) { showToast('❌ 请输入以 http:// 或 https:// 开头的链接', 'error'); return; }
    wrapSelection(`![${alt}](`, `${url})`);
  },
  table: () => {
    const tbl = `\n| 列1 | 列2 | 列3 |\n| --- | --- | --- |\n| 内容 | 内容 | 内容 |\n`;
    const pos = editor.selectionEnd;
    editor.setRangeText(tbl, pos, pos, 'end');
    editor.focus();
    renderPreview();
    pushHistory(editor.value);
  },
  undo: () => undo(),
  redo: () => redo(),
};

document.querySelectorAll('.tool-btn[data-action]').forEach((btn) => {
  btn.addEventListener('click', () => {
    const action = btn.dataset.action;
    if (TOOLBAR_ACTIONS[action]) TOOLBAR_ACTIONS[action]();
  });
});

// --- Toggle preview ---
let previewVisible = true;
document.getElementById('btn-toggle-preview').addEventListener('click', function () {
  previewVisible = !previewVisible;
  if (previewVisible) {
    editorContainer.classList.remove('preview-only');
    this.classList.add('active');
    this.textContent = '👁 预览';
  } else {
    editorContainer.classList.add('preview-only');
    this.classList.remove('active');
    this.textContent = '📝 编辑';
  }
});

// --- Fullscreen toggle ---
let fullscreen = false;
document.getElementById('btn-fullscreen').addEventListener('click', function () {
  fullscreen = !fullscreen;
  if (fullscreen) {
    editorContainer.classList.add('editor-only');
    this.textContent = '⛶ 退出全屏';
  } else {
    editorContainer.classList.remove('editor-only');
    this.textContent = '⛶ 全屏';
  }
});

// --- Resize handle ---
(function initResizer() {
  const handle     = document.getElementById('resize-handle');
  const editorPane = document.getElementById('editor-pane');
  const previewPane = document.getElementById('preview-pane');
  let dragging = false;
  let startX, startEditorW, totalW;

  handle.addEventListener('mousedown', (e) => {
    dragging = true;
    startX = e.clientX;
    startEditorW = editorPane.getBoundingClientRect().width;
    totalW = editorContainer.getBoundingClientRect().width - handle.offsetWidth;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  });

  document.addEventListener('mousemove', (e) => {
    if (!dragging) return;
    const delta = e.clientX - startX;
    const newW  = Math.min(Math.max(startEditorW + delta, 160), totalW - 160);
    editorPane.style.flex  = `0 0 ${newW}px`;
    previewPane.style.flex = `0 0 ${totalW - newW}px`;
  });

  document.addEventListener('mouseup', () => {
    if (dragging) {
      dragging = false;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }
  });
})();

// --- Keyboard shortcuts ---
editor.addEventListener('keydown', (e) => {
  const ctrl = e.ctrlKey || e.metaKey;
  if (ctrl && e.key === 's') { e.preventDefault(); saveFile(); }
  if (ctrl && e.key === 'b') { e.preventDefault(); TOOLBAR_ACTIONS.bold(); }
  if (ctrl && e.key === 'i') { e.preventDefault(); TOOLBAR_ACTIONS.italic(); }
  if (ctrl && !e.shiftKey && e.key === 'z') { e.preventDefault(); undo(); }
  if (ctrl && (e.key === 'y' || (e.shiftKey && e.key === 'z'))) { e.preventDefault(); redo(); }

  // Auto-indent: Tab key inserts 2 spaces
  if (e.key === 'Tab') {
    e.preventDefault();
    const start = editor.selectionStart;
    editor.setRangeText('  ', start, start, 'end');
    pushHistory(editor.value);
  }
});

// --- Live preview on input ---
// Debounce history pushes to avoid recording every keystroke
let historyTimer = null;
editor.addEventListener('input', () => {
  renderPreview();
  updateStatus();
  clearTimeout(historyTimer);
  historyTimer = setTimeout(() => pushHistory(editor.value), 500);
});

// --- Prevent accidental data loss ---
window.addEventListener('beforeunload', (e) => {
  if (editor.value && editor.value !== history[0]) {
    e.preventDefault();
    e.returnValue = '';
  }
});

// --- Header buttons ---
document.getElementById('btn-save').addEventListener('click', saveFile);
document.getElementById('btn-new').addEventListener('click', newFile);
document.getElementById('btn-open').addEventListener('click', openFileModal);
document.getElementById('btn-delete').addEventListener('click', async () => {
  const name = filenameInput.value.trim() || 'untitled';
  await deleteFile(`${name}.md`);
  newFile();
});

// --- Modal close ---
document.getElementById('modal-close').addEventListener('click', closeModal);
document.querySelector('.modal-backdrop').addEventListener('click', closeModal);
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeModal();
});

// --- Init ---
const WELCOME_MD = `# 欢迎使用 Markdown 编辑器 👋

这是一个功能完整的 **Markdown 编辑器**，支持实时预览。

## 功能特性

- ✅ 实时渲染预览
- ✅ 语法高亮（代码块）
- ✅ 常用格式工具栏
- ✅ 文件保存 / 打开 / 删除
- ✅ 拖拽调整编辑区与预览区宽度
- ✅ 键盘快捷键（\`Ctrl+S\` 保存，\`Ctrl+B\` 加粗，\`Ctrl+I\` 斜体）

## 快速开始

在左侧输入 Markdown 内容，右侧会**实时**展示渲染效果。

\`\`\`javascript
// 示例代码
const greeting = "Hello, Markdown!";
console.log(greeting);
\`\`\`

> 💡 提示：使用顶部工具栏快速插入格式，或直接输入 Markdown 语法。

| 快捷键 | 功能 |
| --- | --- |
| Ctrl+S | 保存文件 |
| Ctrl+B | 加粗 |
| Ctrl+I | 斜体 |
| Tab | 缩进（2空格）|
`;

editor.value = WELCOME_MD;
pushHistory(WELCOME_MD);
renderPreview();
updateStatus();
editor.focus();
