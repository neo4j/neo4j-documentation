require 'asciidoctor/extensions'

class DeprecationAdmonition < Asciidoctor::Extensions::BlockProcessor

  use_dsl

  named :DEPRECATED
  on_contexts :example, :paragraph, :open

  def process parent, reader, attrs
    attrs['name'] = 'caution'
    attrs['caption'] = 'Deprecated'
    attrs['role'] = 'deprecated'
    block = create_block parent, :admonition, reader.lines, attrs, content_model: :compound
  end

end
