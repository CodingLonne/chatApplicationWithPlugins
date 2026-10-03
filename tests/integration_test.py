"""End-to-end checks; no third-party Python packages required."""
import codecs
import pathlib
import queue
import re
import select
import socket
import subprocess
import tempfile
import threading

ROOT = pathlib.Path(__file__).resolve().parents[1]


def encrypt(text):
    return codecs.encode(text, 'rot_13')[::-1]


def decrypt(text):
    return codecs.decode(text[::-1], 'rot_13')


class Peer:
    def __init__(self, port):
        self.socket = socket.create_connection(('localhost', port), timeout=5)
        self.input = self.socket.makefile('rb')

    def send(self, text):
        self.socket.sendall((encrypt(text) + '\n').encode('utf-8'))

    def receive(self):
        wire = self.input.readline()
        if not wire:
            return None
        wire = wire.decode('utf-8').rstrip('\r\n')
        plain = decrypt(wire)
        assert wire == encrypt(plain)
        assert wire != plain, 'Test frame unexpectedly transmitted as plaintext'
        return plain

    def close(self):
        self.input.close()
        self.socket.close()


def capture(process):
    lines = queue.Queue()

    def read():
        for line in process.stdout:
            lines.put(line)

    threading.Thread(target=read, daemon=True).start()
    return lines


def wait_for(lines, text):
    while True:
        line = lines.get(timeout=10)
        if text in line:
            return line


def main():
    assert encrypt('Hello') == 'byyrU'
    # Invoke the JVM directly: Windows javapath shims can leave a child running
    # when their parent process is terminated during test cleanup.
    settings = subprocess.run(['java', '-XshowSettings:properties', '-version'],
                              capture_output=True, text=True, check=True)
    java_home = re.search(r'^\s*java.home = (.+)$', settings.stderr, re.MULTILINE)
    assert java_home, 'Could not determine the Java runtime directory'
    java = str(pathlib.Path(java_home.group(1).strip()) / 'bin' / 'java')
    processes, peers = [], []
    with tempfile.TemporaryDirectory(prefix='chat-test-') as folder:
        work = pathlib.Path(folder)
        classes = work / 'bin'
        classes.mkdir()
        subprocess.run(['javac', '--release', '8', '-Xlint:all', '-encoding', 'UTF-8',
                        '-d', str(classes)] + [str(p) for p in (ROOT / 'src').glob('*.java')],
                       check=True)
        try:
            server = subprocess.Popen([java, '-cp', str(classes), 'ChatServer', '0'],
                                      cwd=work, stdout=subprocess.PIPE,
                                      stderr=subprocess.STDOUT, text=True, encoding='utf-8')
            processes.append(server)
            port = int(wait_for(capture(server), 'listening on port').split()[-1])

            def peer():
                result = Peer(port)
                peers.append(result)
                return result

            def login(name):
                result = peer()
                result.send('AUTH\t' + name + '\tspl-chat')
                assert result.receive() == 'OK\tAuthenticated'
                return result

            wrong = peer()
            wrong.send('AUTH\tMallory\twrong')
            assert wrong.receive().startswith('ERROR\t')
            assert wrong.receive() is None
            early = peer()
            early.send('CHAT\tred\tunauthorized')
            assert early.receive().startswith('ERROR\t')
            assert early.receive() is None
            waiting = peer()
            alice, bob = login('Alice'), login('Bob')
            alice.send('CHAT\tred\tHello Bob!')
            expected = 'CHAT\tAlice\tred\tHello Bob!'
            assert alice.receive() == bob.receive() == expected
            assert not select.select([waiting.socket], [], [], 0.25)[0]

            bob.send('CHAT\tblue\tHej, världen! 😀\twith tab')
            expected = 'CHAT\tBob\tblue\tHej, världen! 😀\twith tab'
            assert alice.receive() == bob.receive() == expected
            bob.send('CHAT\tbad color\tinvalid')
            assert bob.receive().startswith('ERROR\t')
            bob.send('CHAT\tblue\t')
            assert bob.receive().startswith('ERROR\t')
            bob.send('AUTH\tBob\tspl-chat')
            assert bob.receive().startswith('ERROR\t')

            client = subprocess.Popen([java, '-cp', str(classes), 'ChatClient',
                                       'localhost', str(port), 'Carol', 'purple'],
                                      cwd=work, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                      stderr=subprocess.STDOUT, text=True, encoding='utf-8')
            processes.append(client)
            lines = capture(client)
            client.stdin.write('spl-chat\n')
            client.stdin.flush()
            wait_for(lines, 'Connected.')
            client.stdin.write('initial color\n')
            client.stdin.flush()
            expected = 'CHAT\tCarol\tpurple\tinitial color'
            assert alice.receive() == bob.receive() == expected
            wait_for(lines, '[purple] Carol: initial color')
            client.stdin.write('/color green\nchanged color\n')
            client.stdin.flush()
            expected = 'CHAT\tCarol\tgreen\tchanged color'
            assert alice.receive() == bob.receive() == expected
            wait_for(lines, '[green] Carol: changed color')
            client.stdin.write('/quit\n')
            client.stdin.flush()
            assert client.wait(timeout=5) == 0
            client_log = next(work.glob('client-Carol-*.log')).read_text(encoding='utf-8')
            assert 'OK\tAuthenticated' in client_log
            assert 'CHAT\tCarol\tpurple\tinitial color' in client_log
            assert expected in client_log

            bob.send('QUIT')
            assert bob.receive() is None
            alice.send('CHAT\tgold\tStill connected')
            assert alice.receive() == 'CHAT\tAlice\tgold\tStill connected'
            server_log = (work / 'server.log').read_text(encoding='utf-8')
            for entry in ['AUTH\tMallory\t[password redacted]',
                          'CHAT\tred\tunauthorized', 'CHAT\tred\tHello Bob!',
                          'CHAT\tblue\tHej, världen! 😀\twith tab',
                          'CHAT\tgreen\tchanged color', 'QUIT',
                          'CHAT\tgold\tStill connected']:
                assert entry in server_log, entry
            assert 'spl-chat' not in server_log
            print('PASS: compilation, authentication, encryption, isolation, broadcast,')
            print('Unicode, validation, disconnects, client color changes and all logs.')
        finally:
            for item in peers:
                item.close()
            for process in reversed(processes):
                if process.poll() is None:
                    process.terminate()
                process.wait(timeout=5)
                if process.stdin:
                    process.stdin.close()
                process.stdout.close()


if __name__ == '__main__':
    main()
