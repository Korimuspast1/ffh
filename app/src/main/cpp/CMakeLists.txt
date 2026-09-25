cmake_minimum_required(VERSION 3.22.1)
project(ffh_launcher C)

# Small launcher that starts the Xray binary and keeps the VPN file descriptor
# open across exec. Android's ProcessBuilder closes every fd except 0/1/2, so
# a Java Process cannot hand the TUN fd to the core.
add_library(ffh SHARED ffh_exec.c)

target_link_options(ffh PRIVATE "-Wl,-z,max-page-size=16384")
