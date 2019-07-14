# node-syncer
(Inefficiently) Pushes missing TX from one node to another

Has 2 options: sync milestone TX, or sync transactions in the transactionrequester(queue) list

Start by building a jar, then run the following:

`java -jar node-syncer.jar [target-node] [source-node-1] [source-node-x]`

Youll be prompted for an option; `milestones` or `queue`.
Type that option, and it starts!
