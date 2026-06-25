# Knowledge base (RAG)

Drop plain-text documentation here as `.md` or `.txt` files — product manuals,
algorithm explanations, FAQs, SOPs. Each file becomes one or more retrievable
chunks (split on blank lines); the filename (dashes/underscores → spaces) is used
as the citation title.

After adding or editing files, rebuild the index:

```
curl -X POST http://localhost:8090/api/assistant/reindex
```

On first startup the orchestrator ingests this folder automatically if the Qdrant
collection is empty.
