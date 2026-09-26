import sys
import importlib.util
from pathlib import Path

# Add project root and data-platform to sys.path
root_dir = Path(__file__).resolve().parent.parent.parent
data_platform_dir = Path(__file__).resolve().parent.parent

if str(root_dir) not in sys.path:
    sys.path.insert(0, str(root_dir))
if str(data_platform_dir) not in sys.path:
    sys.path.insert(0, str(data_platform_dir))

# Alias 'data-platform' directory to 'data_platform' module name
if "data_platform" not in sys.modules:
    spec = importlib.util.spec_from_file_location(
        "data_platform",
        str(data_platform_dir / "__init__.py"),
        submodule_search_locations=[str(data_platform_dir)],
    )
    if spec and spec.loader:
        module = importlib.util.module_from_spec(spec)
        sys.modules["data_platform"] = module
        spec.loader.exec_module(module)
