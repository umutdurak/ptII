# $Id$
# The name of the FMU to be built.
FMU_NAME = Influenza

#CFLAGS = -I/opt/openmodelica/include
#CFLAGS = -I/opt/local/include/omc/c 
CFLAGS = -I/opt/openmodelica/include -llapack -lblas -L/opt/local/lib/gcc49 -lgfortran

# The rules are in a separate file so that we can easily update them.
include fmu.mk
