export default function TagsView({tags = [], activeTagId, onSelect, onClose}) {
  return (
      <nav className="hb-tags-view" aria-label="已打开页面">
          {tags.map((tag) => {
              const tagKey = tag.key || tag.id
              const title = tag.title || tag.name || tagKey
              const active = tagKey === activeTagId
              return (
                  <div key={tagKey} className={`hb-tag ${active ? 'active' : ''}`}>
                      <button
                          type="button"
                          className="hb-tag-main"
                          aria-current={active ? 'page' : undefined}
                          title={tag.path || tagKey}
                          onClick={() => onSelect?.(tag)}
                      >
                          <span>{title}</span>
                      </button>
                      {tag.closable !== false && (
                          <button
                              type="button"
                              className="hb-tag-close"
                              aria-label={`关闭 ${title}`}
                              title={`关闭 ${title}`}
                              onClick={() => onClose?.(tagKey)}
                          >
                              <span aria-hidden="true">×</span>
                          </button>
                      )}
                  </div>
              )
          })}
      </nav>
  )
}
