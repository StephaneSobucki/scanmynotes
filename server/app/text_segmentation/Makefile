CXX=g++

SRCDIR=src
SRCEXT=cpp
OBJDIR=obj
TARGETDIR=bin
TARGET= $(TARGETDIR)/text_segmentation
SOURCES=$(shell find $(SRCDIR) -type f -name *.$(SRCEXT))
OBJECTS=$(patsubst $(SRCDIR)/%,$(OBJDIR)/%,$(SOURCES:.$(SRCEXT)=.o))
OPENCV = `pkg-config --cflags --libs opencv`
LIBS=$(OPENCV)
CXXFLAGS=-lstdc++fs

$(TARGET) : $(OBJECTS)
	@mkdir -p $(TARGETDIR)
	$(CXX) $^ $(LIBS) -o $(TARGET) $(CXXFLAGS)

$(OBJDIR)/%.o : $(SRCDIR)/%.$(SRCEXT)
	@mkdir -p $(OBJDIR)
	$(CXX) -c $^ -o $@

clean:
	rm -r $(OBJDIR)
	rm -r $(TARGETDIR)
