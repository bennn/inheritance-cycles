import log

from util.graph import Graph, Node, Edge
from lxml import etree

class AstXmlParser:
    
    def __init__(self, fname):
        """
            2013-07-24:
                Create a graph of the file
        """
        self.astGraph = Graph()
        self.processFile(fname)

    def getGraph(self):
        return self.astGraph

    def processElement(self, xxs, projectName):
        """
            2013-07-22:
                process one <file> element from the output
        """
        filename = self._filename(xxs)
        for el in self._elements(xxs):
            edges = self._edges(el)
            name = self._elementName(el)
            kind = self._kind(el)
            node = Node(name, edge_list=edges, meta={"kind": kind})
            self.astGraph.add(node)

    def processFile(self, filename):
        """
            2013-08-17:
                Iterative xml parsing, adapted from:
                    http://www.ibm.com/developerworks/library/x-hiperfparse/
        """
        projectName = self._projectName(filename)
        stack = []
        context = etree.iterparse(filename, events=("start", "end"), tag=["file"])
        for event, elem in context:
            if event == "start":
                stack.append(elem.tag)
            elif stack.pop() != elem.tag:
                log.error("Unexpected empty stack during xml parsing")
            else:
                try:
                    self.processElement(elem, projectName)
                except Exception as exc:
                    log.error("Error during xml parsing: %s" % exc)
                if len(stack) == 0:  # element is safe to delete
                    elem.clear()
                    while elem.getprevious() is not None:
                        del elem.getparent()[0]
        del context

    def _edges(self, xxs, supertype=None):
        """
            2013-07-22:
                Edges are to the supertypes of a node,
                supertypes may have type arguments
        """
        edges = []
        supers = False
        for el in xxs.xpath("./supertypes/supertype"):
            supers = True
            name = el.xpath("./@name")[0]
            edge = Edge(name)
            edges.append(edge)
            # Get all type arguments to the supertype
            # This call should bypass the first for loop (supertypes)
            # and instead enter the second (type-args)
            for e in self._edges(el, supertype=name):
                edges.append(e)
        for el in xxs.xpath("./type-args/type"):
            if supers:
                log.error("Element %s has both supertypes and type arguments on the same level." % self._elementName(xxs))
                supers = False
            name = el.xpath("./@name")[0]
            if supertype is None:
                log.error("Type argument %s has no supertype." % name)
            edge = Edge(name, label=supertype)
            edges.append(edge)
        return edges

    def _elementName(self, xxs):
        return xxs.xpath("./expanded-name/@name")[0]

    def _elements(self, xxs):
        return xxs.xpath(".//element")

    def _filename(self, xxs):
        return xxs.xpath("./@local-name")[0]

    def _kind(self, xxs):
        """
            2013-08-17:
                Examples of kinds: CLASS, TYPE, PACKAGE
        """
        return xxs.xpath("./kind/text()")[0]

    def _projectName(self, filename):
        """
            2013-07-23:
                Get the value of the 'project' attribute in the
                <files> tag
                (there should only be one <files> tag)
        """
        _, elem = next(etree.iterparse(filename, tag="files"))
        results = elem.xpath("./@project")
        if results != []:
            return results[0]
        else:
            return "UNDEF"

    def _typeArguments(self, xxs):
        """
            2013-07-22:
                Return the type arguments of an ast node
                If there's a node N<K,V>, the type arguments will be [K,V]
                For node N<M<K>>, type arguments are [M,K]
        """
        return xxs.xpath("./expanded-name/type-args//type/@name")
