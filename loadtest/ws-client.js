// 简易通知 WebSocket 客户端(Node 22+ 内置 WebSocket)
// 用法: node ws-client.js <token> [port] [waitMs] [expectedType]
const token = process.argv[2];
const port = process.argv[3] || '8084';
const waitMs = Number(process.argv[4] || 20000);
const expected = process.argv[5] || '';

if (!token) {
    console.log('MISSING_TOKEN');
    process.exit(1);
}

const ws = new WebSocket(`ws://localhost:${port}/ws/notifications?token=${token}`);
const timer = setTimeout(() => {
    console.log('TIMEOUT');
    process.exit(2);
}, waitMs);

ws.addEventListener('open', () => {
    console.log('OPEN');
});

ws.addEventListener('message', (event) => {
    const text = String(event.data);
    console.log('MESSAGE ' + text);
    if (!expected || text.includes(expected)) {
        clearTimeout(timer);
        try { ws.close(); } catch (e) { /* ignore */ }
        process.exit(0);
    }
});

ws.addEventListener('error', () => {
    console.log('ERROR');
});

ws.addEventListener('close', () => {
    console.log('CLOSED');
});
