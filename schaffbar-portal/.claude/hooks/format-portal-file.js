// PostToolUse hook (Edit|Write|MultiEdit): formats a file of the portal with Prettier right after it was written –
// this also sorts its imports (prettier-plugin-organize-imports), so `npm run check` does not fail on them later.
// Reads the hook input from stdin; every other file is left alone.
const { execFileSync } = require('node:child_process');

let input = '';
process.stdin.on('data', (chunk) => (input += chunk));
process.stdin.on('end', () => {
  const filePath = JSON.parse(input).tool_input?.file_path ?? '';
  const match = filePath.match(/^(.*\/schaffbar-portal)\/(src|public)\/.+\.(ts|html|scss|json)$/);
  if (!match) return;

  execFileSync('npx', ['prettier', '--write', '--log-level', 'warn', filePath], { cwd: match[1], stdio: 'inherit' });
});
