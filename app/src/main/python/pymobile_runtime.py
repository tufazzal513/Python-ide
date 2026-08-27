import sys
import io
import os
import traceback
import shlex
import importlib.metadata

class JavaStreamRedirector:
    def __init__(self, callback):
        self.callback = callback
        self.buffer = ""

    def write(self, s):
        if s is None:
            return 0
        text = str(s)
        if not text:
            return 0
        self.buffer += text
        while "\n" in self.buffer:
            line, self.buffer = self.buffer.split("\n", 1)
            try:
                self.callback.onOutput(line)
            except Exception:
                pass
        return len(text)

    def writelines(self, lines):
        for line in lines:
            self.write(line)

    def flush(self):
        if self.buffer:
            try:
                self.callback.onOutput(self.buffer)
            except Exception:
                pass
            self.buffer = ""

    def isatty(self):
        return False

    def readable(self):
        return False

    def writable(self):
        return True

    def seekable(self):
        return False


def run_python_file(file_path, project_dir, stdout_cb, stderr_cb, cancel_checker=None, env_vars=None, args=""):
    """
    Executes a Python script file in the project context with full stdout/stderr capture
    and cooperative cancellation via trace function.
    """
    orig_stdout = sys.stdout
    orig_stderr = sys.stderr
    orig_argv = list(sys.argv)
    orig_cwd = os.getcwd()

    stdout_redir = JavaStreamRedirector(stdout_cb)
    stderr_redir = JavaStreamRedirector(stderr_cb)

    sys.stdout = stdout_redir
    sys.stderr = stderr_redir

    # Update sys.path
    if project_dir:
        site_packages = os.path.join(project_dir, "site-packages")
        if os.path.exists(site_packages) and site_packages not in sys.path:
            sys.path.insert(0, site_packages)
        if project_dir not in sys.path:
            sys.path.insert(0, project_dir)
        try:
            os.chdir(project_dir)
        except Exception:
            pass

    # Environment variables
    if env_vars:
        for k, v in env_vars.items():
            os.environ[str(k)] = str(v)

    # sys.argv
    new_argv = [file_path]
    if args:
        try:
            new_argv.extend(shlex.split(args))
        except Exception:
            new_argv.extend(args.split())
    sys.argv = new_argv

    # Trace for cooperative cancellation
    def trace_cancel(frame, event, arg):
        if cancel_checker is not None:
            try:
                if cancel_checker.isCancelled():
                    raise KeyboardInterrupt("Execution stopped by user")
            except Exception:
                pass
        return trace_cancel

    if cancel_checker is not None:
        sys.settrace(trace_cancel)

    exit_code = 0
    try:
        with open(file_path, "rb") as f:
            code_bytes = f.read()

        code_obj = compile(code_bytes, file_path, "exec")
        global_scope = {
            "__name__": "__main__",
            "__file__": file_path,
            "__doc__": None,
            "__builtins__": __builtins__,
        }
        exec(code_obj, global_scope)
        exit_code = 0
    except SystemExit as se:
        if se.code is None:
            exit_code = 0
        elif isinstance(se.code, int):
            exit_code = se.code
        else:
            stderr_redir.write(f"{se.code}\n")
            exit_code = 1
    except KeyboardInterrupt:
        stderr_redir.write("\n[PyMobile] Process interrupted (KeyboardInterrupt).\n")
        exit_code = 130
    except Exception:
        exc_text = traceback.format_exc()
        stderr_redir.write(exc_text + "\n")
        exit_code = 1
    finally:
        sys.settrace(None)
        stdout_redir.flush()
        stderr_redir.flush()
        sys.stdout = orig_stdout
        sys.stderr = orig_stderr
        sys.argv = orig_argv
        try:
            os.chdir(orig_cwd)
        except Exception:
            pass

    return exit_code


def eval_python_code(code_str, project_dir, stdout_cb, stderr_cb, env_vars=None):
    """
    Executes an inline Python code snippet or expression.
    """
    orig_stdout = sys.stdout
    orig_stderr = sys.stderr
    orig_cwd = os.getcwd()

    stdout_redir = JavaStreamRedirector(stdout_cb)
    stderr_redir = JavaStreamRedirector(stderr_cb)

    sys.stdout = stdout_redir
    sys.stderr = stderr_redir

    if project_dir:
        site_packages = os.path.join(project_dir, "site-packages")
        if os.path.exists(site_packages) and site_packages not in sys.path:
            sys.path.insert(0, site_packages)
        if project_dir not in sys.path:
            sys.path.insert(0, project_dir)
        try:
            os.chdir(project_dir)
        except Exception:
            pass

    if env_vars:
        for k, v in env_vars.items():
            os.environ[str(k)] = str(v)

    exit_code = 0
    try:
        global_scope = {
            "__name__": "__main__",
            "__builtins__": __builtins__,
        }
        try:
            compiled_eval = compile(code_str, "<stdin>", "eval")
            res = eval(compiled_eval, global_scope)
            if res is not None:
                print(repr(res))
        except SyntaxError:
            compiled_exec = compile(code_str, "<stdin>", "exec")
            exec(compiled_exec, global_scope)
        exit_code = 0
    except SystemExit as se:
        exit_code = se.code if isinstance(se.code, int) else (0 if se.code is None else 1)
    except KeyboardInterrupt:
        stderr_redir.write("\nKeyboardInterrupt\n")
        exit_code = 130
    except Exception:
        exc_text = traceback.format_exc()
        stderr_redir.write(exc_text + "\n")
        exit_code = 1
    finally:
        stdout_redir.flush()
        stderr_redir.flush()
        sys.stdout = orig_stdout
        sys.stderr = orig_stderr
        try:
            os.chdir(orig_cwd)
        except Exception:
            pass

    return exit_code


def list_installed_packages(project_dir=None):
    """
    Returns formatted list of installed packages in Chaquopy runtime and project site-packages.
    """
    packages = {}
    
    # 1. Standard importlib metadata
    try:
        for dist in importlib.metadata.distributions():
            packages[dist.metadata['Name'].lower()] = (dist.metadata['Name'], dist.version)
    except Exception:
        pass

    # 2. Project site-packages
    if project_dir:
        site_packages = os.path.join(project_dir, "site-packages")
        if os.path.exists(site_packages):
            for entry in os.listdir(site_packages):
                if entry.endswith(".dist-info") and "-" in entry:
                    name_ver = entry[:-len(".dist-info")]
                    parts = name_ver.rsplit("-", 1)
                    if len(parts) == 2:
                        pname, pver = parts
                        packages[pname.lower()] = (pname, pver)

    return sorted(packages.values(), key=lambda x: x[0].lower())
