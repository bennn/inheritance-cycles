class Edge:
    def __init__(self, dest, label=None):
        self.dest = dest
        self.label = label

    def pprint(self):
        return "<%s,%s>" % (self.dest, self.label)

class Node:

    def __init__(self, id, edge_list=[], meta=None):
        self.id = id
        self.edge_list = edge_list
        self.meta = meta

    def pprint(self):
        return {
            "id": self.id,
            "meta": self.meta,
            "edges": [e.pprint() for e in self.edge_list]
        }

class Graph:

    graph = {}

    def add(self, node):
        self.graph[node.id] = node
        for edge in node.edge_list:
            if edge.dest not in self.graph:
                self.graph[edge.dest] = Node(edge.dest)

    def exists(self, nodeId):
        return nodeId in self.graph

    def find_cycles(self):
        """
            2013-07-22:
                Find all the cycles in a graph. 
                Algorithm due to Donald B. Johnson
        """
        # Block nodes when all paths to the root intersects current path
        # at some node `n != s`
        # All vertices start off unblocked
        blocked = dict( (id, False) for id in self.graph.iterkeys() )
        B = dict((id, []) for id in self.graph.iterkeys() )
        cycles = []
        stack = []
        # Current vertex we're searching from. We iterate over graph, eventually
        s = None
        def unblock(n):
            """
                Unblock a node + some of its children. "Unblocking is always
                delayed such that two unblockings of `v` are separated by either
                output of a new circuit or return to main.
            """
            blocked[n] = False
            for w in B[n]:
                # remove w from B[n]
                B[n].remove(w)
                if blocked[w]:
                    unblock(w)
            return
        def circuit(edge):
            """
                Find circuits starting from `edge.dest`.
                Invariant: Initial node must have edge labeled "INIT"

                TODO need to track labels of the edges. Need the names of
                cycle-enablers
            """
            f = False
            v = edge.dest
            # add edge (dest+label) to stack
            stack.append(edge)
            blocked[v] = True
            for e in self.graph[v].edge_list:
                w = e.dest
                if w == s:
                    stack.append(Edge(s, e.label))
                    # output circuit: stack + s + how we got to s
                    cycles.append(list(stack))
                    stack.pop()
                    f = True
                elif not blocked[w]:
                    f = circuit(e)
            if f:
                unblock(v)
            else:
                for e in self.graph[v].edge_list:
                    w = e.dest
                    if v not in B[w]:
                        B[w].append(v)
            # Remove v from stack. Popping seems to accomplish this, but I'm wary
            # print("Stack[-1] = %s , v = %s" % (stack[-1], v))
            stack.pop()
            return f
        # Iterate over nodes
        for s in self.graph.iterkeys():
            # Adjacency structure of strong compoenet K with
            # least vertex in subgraph of G denoted by {s, s+1, ... n}
            if len(self.graph) > 0:
                # Find all circuits starting at `s`
                circuit(Edge(s, label="INIT"))
            else:
                break
        return cycles

    def get(self, nodeId):
        """
            2013-07-22:
                Return node identified by `nodeId`, create it
                if it doesn't already exist.
        """
        if self.exists(nodeId):
            return self.graph['nodeId']
        else:
            newNode = Node(nodeId)
            self.graph[nodeId] = newNode
            return newNode

    def print_cycles(self, cycle_list):
        """
            2013-07-24:
                Stringify output from `find_cycles`
        """
        lines  = []
        for cycle in cycle_list:
            lines.append("[%s]" % ";".join(( e.pprint() for e in cycle )))
        return "\n".join(lines or ["No Cycles!"])

    def pprint(self):
        # Hmmm
        return("\n".join([
            node.pprint()
            for (_, node) in self.graph.iteritems()
        ]))
