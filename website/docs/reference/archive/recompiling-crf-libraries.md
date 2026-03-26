<h1>Recompiling and integrating CRF libraries</h1>

Grobid can be used with two different CRF libraries: Wapiti (the default and most performant one) and CRF++ (the historical one). However, CRF++ is deprecated and we are not considering it anymore. Wapiti appears two time faster for decoding than CRF++ for the Grobid models. The sizes of the Wapiti models are five to ten times smaller in memory than CRF++ ones. Training time is also significantly reduced based on the Wapiti `l-bfgs` training algorithm with a similar accuracy. 

## Wapiti

Wapiti is the default CRF library used by Grobid. It is integrated transparently to Grobid via JNI and there is normally nothing to be additionally done. This section explains how to rebuild and install the native library for integrating into Grobid a new versions of Wapiti.

For Grobid, we are using a specific fork of the original Wapiti distribution: [Wapiti fork for Grobid](https://github.com/kermitt2/Wapiti).

This version includes in particular SWIG mapping providing a JNI interface and some bug fixes. 

**1) Build**

The instruction for building the library are given on the above GitHub repo. The libraries `libwapiti.so` or `libwapiti.dylib` are then available under the subdirection `build/`. 

**2) Portability**

On Mac OS X architecture, `libwapiti.dylib` is already portable. 

On Linux, for having the library portable and not dynamically linking to the installed system libraries, process as follow: 

+ in the subdirection `build/`, execute the provided script to collect the dependencies
```bash
> ../collect-dependencies.sh libwapiti.so .
```
The script will copy the required libaries together with `libwapiti.so`.

+ The local linking will be prioritize to ensure portability of the JNI, which can be checked on Linux by:
```bash
> ldd libwapiti.so
```
which should display something like that, with linking only to the local dependencies:

	linux-vdso.so.1 =>  (0x00007fff8e591000)
	libstdc++.so.6 => /home/at-sac/plopez/Wapiti/build/./libstdc++.so.6 (0x00007f6a0e46b000)
	libm.so.6 => /home/at-sac/plopez/Wapiti/build/./libm.so.6 (0x00007f6a0e1e6000)
	libgcc_s.so.1 => /home/at-sac/plopez/Wapiti/build/./libgcc_s.so.1 (0x00007f6a0dfd0000)
	libc.so.6 => /home/at-sac/plopez/Wapiti/build/./libc.so.6 (0x00007f6a0dc3c000)
	/lib64/ld-linux-x86-64.so.2 (0x00000032a0c00000)

**3) Install native libraries**

The next step is to install the updated libraries in the Grobid distribution. On Linux:
```bash
> cp ld-linux-x86-64.so.2 libc.so.6 libgcc_s.so.1 libm.so.6 libstdc++.so.6 libwapiti.so GROBID-ROOT-DIRECTORY/grobid-home/lib/lin-<nb bits of the OS>
```
On Max OS X: 
```bash
	> cp libwapiti.dylib GROBID-ROOT-DIRECTORY/grobid-home/lib/mac-64/
```
**4) Install the JNI jar**

Finally, the JNI jar file has to be deployed in the local repository in grobid-core (we suppose here that the version of Wapiti is 1.5.0, to be adapted if necessary):
```bash
> mvn install:install-file -Dfile=wapiti-1.5.0.jar -DgroupId="fr.limsi.wapiti" -DartifactId="wapiti" -Dversion="1.5.0" -Dpackaging="jar" -DlocalRepositoryPath="GROBID-ROOT-DIRECTORY/grobid-core/lib"
```
If the Wapiti library version changes, the dependency version in grobid-core/pom.xml has to be updated.


## CRF++

The usage of CRF++ is not supported anymore since GROBID version 0.4.

### Integration to GROBID source

The generated library has to be added in the open source project. Copy libcrfpp.so (See previous paragraph) and all linked libraries to `GROBID-ROOT-DIRECTORY/grobid-home/lib/lin-<nb bits of the OS>`, for instance:
```bash
> cp ld-linux-x86-64.so.2 libwapiti.so libc.so.6 libgcc_s.so.1 libm.so.6 libpthread.so.0 libstdc++.so.6 GROBID-ROOT-DIRECTORY/grobid-home/lib/lin-<nb bits of the OS>
```
The Java dependency file has to be deployed in the local repository for grobid-core. Finally the dependency version of wapiti in `build.gradle` has to be updated.
