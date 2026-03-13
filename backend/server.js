'use strict';

const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const FILES_DIR = path.join(__dirname, 'files');

// Ensure the files directory exists
if (!fs.existsSync(FILES_DIR)) {
  fs.mkdirSync(FILES_DIR, { recursive: true });
}

app.use(cors());
app.use(express.json({ limit: '1mb' }));

// Serve the frontend
app.use(express.static(path.join(__dirname, '..', 'frontend')));

// --- Helper ---

/**
 * Sanitize a filename so it only contains safe characters.
 * Returns null when the name is invalid.
 * @param {string} name
 * @returns {string|null}
 */
function sanitizeFilename(name) {
  if (typeof name !== 'string' || name.trim() === '') return null;
  // Only allow alphanumerics, hyphens, underscores, and Chinese characters (no dots in user input)
  const safe = name.trim().replace(/[^\w\u4e00-\u9fa5\-]/g, '_');
  // Reject names starting with a dot (hidden files) or containing traversal sequences
  if (safe.startsWith('.') || safe.includes('..') || path.basename(safe) !== safe) return null;
  // Enforce .md extension
  return `${safe}.md`;
}

// --- API Routes ---

/**
 * GET /api/files
 * List all saved Markdown files.
 */
app.get('/api/files', (req, res) => {
  try {
    const files = fs.readdirSync(FILES_DIR)
      .filter((f) => f.endsWith('.md'))
      .map((f) => {
        const stat = fs.statSync(path.join(FILES_DIR, f));
        return {
          name: f,
          updatedAt: stat.mtime.toISOString(),
          size: stat.size,
        };
      })
      .sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
    res.json({ files });
  } catch (err) {
    res.status(500).json({ error: 'Failed to list files.' });
  }
});

/**
 * GET /api/files/:name
 * Load a Markdown file by name.
 */
app.get('/api/files/:name', (req, res) => {
  const filename = sanitizeFilename(req.params.name);
  if (!filename) {
    return res.status(400).json({ error: 'Invalid filename.' });
  }
  const filePath = path.join(FILES_DIR, filename);
  if (!fs.existsSync(filePath)) {
    return res.status(404).json({ error: 'File not found.' });
  }
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    res.json({ name: filename, content });
  } catch (err) {
    res.status(500).json({ error: 'Failed to read file.' });
  }
});

/**
 * POST /api/files
 * Save (create or update) a Markdown file.
 * Body: { name: string, content: string }
 */
app.post('/api/files', (req, res) => {
  const { name, content } = req.body;
  const filename = sanitizeFilename(name);
  if (!filename) {
    return res.status(400).json({ error: 'Invalid filename.' });
  }
  if (typeof content !== 'string') {
    return res.status(400).json({ error: 'Content must be a string.' });
  }
  const filePath = path.join(FILES_DIR, filename);
  try {
    fs.writeFileSync(filePath, content, 'utf8');
    const stat = fs.statSync(filePath);
    res.json({ name: filename, updatedAt: stat.mtime.toISOString() });
  } catch (err) {
    res.status(500).json({ error: 'Failed to save file.' });
  }
});

/**
 * DELETE /api/files/:name
 * Delete a Markdown file by name.
 */
app.delete('/api/files/:name', (req, res) => {
  const filename = sanitizeFilename(req.params.name);
  if (!filename) {
    return res.status(400).json({ error: 'Invalid filename.' });
  }
  const filePath = path.join(FILES_DIR, filename);
  if (!fs.existsSync(filePath)) {
    return res.status(404).json({ error: 'File not found.' });
  }
  try {
    fs.unlinkSync(filePath);
    res.json({ message: 'File deleted.' });
  } catch (err) {
    res.status(500).json({ error: 'Failed to delete file.' });
  }
});

// Fall-through: serve the SPA for any non-API route
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, '..', 'frontend', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Markdown Editor server running at http://localhost:${PORT}`);
});

module.exports = app;
