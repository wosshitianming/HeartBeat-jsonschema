export default function TagsView({ tags, activeTagId, onSelect, onClose }) {
  return (
      <div className="hb-tags-view">
          {tags.map((tag) => {
              const tagKey = tag.key || tag.id
              const title = tag.title || tag.name || tagKey
              return (
                  <div
                      key={tagKey}
                      className={`hb-tag ${tagKey === activeTagId ? 'active' : ''}`}
                      onClick={() => onSelect(tag)}
                      onKeyDown={(event) => {
                          if (event.key === 'Enter') onSelect(tag)
                      }}
                      role="button"
                      tabIndex={0}
                      title={tag.path || tagKey}
                  >
                      <span>{title}</span>
                      {tag.closable !== false && (
                          <button
                              type="button"
                              aria-label={`Close ${title}`}
                              onClick={(event) => {
                                  event.stopPropagation()
                                  onClose(tagKey)
                              }}
                          >
                              x
                          </button>
                      )}
                  </div>
              )
          })}
      </div>
  )
}
