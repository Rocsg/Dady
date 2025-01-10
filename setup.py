from setuptools import setup, find_packages

setup(
    name='Dadypy',
    version='1.0.0',
    description='Dady package',
    author='Romain Fernandez',
    author_email='romain.fernandez@cirad.fr',
    package_dir={"": "python"},  # Indique que les packages sont dans le dossier 'python'
    packages=find_packages(where="python"),
    install_requires=[
        'imagecodecs',
        'numpy',
        'tifffile'
        # Add any other dependencies here
    ],
)

