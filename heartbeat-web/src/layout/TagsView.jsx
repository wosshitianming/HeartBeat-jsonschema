export default function TagsView({ tags, activeTagId, onSelect, onClose }) {
  return (
      <div className="hb-tags-view">
        {tags.map((tag) => (
            <div
                key={tag.id}
                className={`hb-tag ${tag.id === activeTagId ? 'active' : ''}`}
                onClick={() => onSelect(tag.id)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') onSelect(tag.id)
                }}
                role="button"
                tabIndex={0}
            >
              <span>{tag.name}</span>
              {tag.closable !== false && (
                  <button
                      type="button"
                      aria-label={`关闭 ${tag.name}`}
                      onClick={(event) => {
                        event.stopPropagation()
                        onClose(tag.id)
                      }}
                  >
                    ×
                  </button>
              )}
            </div>
        ))}
      </div>
  )
}
